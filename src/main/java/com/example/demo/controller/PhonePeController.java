package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

import com.phonepe.sdk.pg.Env;
import com.phonepe.sdk.pg.payments.v2.StandardCheckoutClient;
import com.phonepe.sdk.pg.payments.v2.models.request.StandardCheckoutPayRequest;
import com.phonepe.sdk.pg.payments.v2.models.response.StandardCheckoutPayResponse;
import com.phonepe.sdk.pg.common.models.response.OrderStatusResponse;
import com.phonepe.sdk.pg.common.exception.PhonePeException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/payment")
public class PhonePeController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${phonepe.client.id}")
    private String clientId;

    @Value("${phonepe.client.secret}")
    private String clientSecret;

    @Value("${phonepe.client.version}")
    private Integer clientVersion;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final Map<String, Map<String, String>> pendingUsers = new ConcurrentHashMap<>();

    private StandardCheckoutClient phonePeClient;

    @PostConstruct
    public void init() {
        System.out.println("PhonePe Client ID: " + clientId);
        phonePeClient = StandardCheckoutClient.getInstance(
            clientId, clientSecret, clientVersion, Env.SANDBOX
        );
    }

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @RequestBody Map<String, String> request) {
        try {
            String name        = request.get("name");
            String phone       = request.get("phoneNumber");
            String email       = request.get("email");
            String password    = request.get("password");
            String plateNumber = request.get("plateNumber");
            String amountStr   = request.get("amount");
            String rcNumber    = request.get("rcNumber");

            double amount = Double.parseDouble(amountStr);
            if (amount < 200) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Minimum amount is ₹200"));
            }

            String merchantOrderId = "ORD_" + UUID.randomUUID()
                    .toString().replace("-", "");

            // ✅ Still store in memory — needed for status check
            Map<String, String> userData = new HashMap<>();
            userData.put("name",        name);
            userData.put("phoneNumber", phone);
            userData.put("email",       email);
            userData.put("password",    password);
            userData.put("plateNumber", plateNumber);
            userData.put("amount",      amountStr);
            userData.put("rcNumber",    rcNumber);
            pendingUsers.put(merchantOrderId, userData);

            long amountInPaise = (long)(amount * 100);

            // ✅ redirectUrl now points to FRONTEND not backend
            String redirectUrl = frontendUrl + "/payment-return?orderId=" + merchantOrderId;

            StandardCheckoutPayRequest payRequest = StandardCheckoutPayRequest.builder()
                .merchantOrderId(merchantOrderId)
                .amount(amountInPaise)
                .redirectUrl(redirectUrl)
                .build();

            StandardCheckoutPayResponse payResponse = phonePeClient.pay(payRequest);
            String paymentUrl = payResponse.getRedirectUrl();

            return ResponseEntity.ok(Map.of(
                "paymentUrl",      paymentUrl,
                "merchantOrderId", merchantOrderId
            ));

        } catch (PhonePeException e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "PhonePe error: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Payment initiation failed: " + e.getMessage()));
        }
    }

    // ✅ Frontend calls this after returning from PhonePe
    // This checks status AND saves user in one shot
    @GetMapping("/verify-and-register")
    public ResponseEntity<Void> verifyAndRegister(
            @RequestParam String merchantOrderId) {
        try {
            System.out.println("🔍 Verifying order: " + merchantOrderId);

            OrderStatusResponse statusResponse = phonePeClient.getOrderStatus(merchantOrderId);
            String state = statusResponse.getState();
            System.out.println("📦 Payment state: " + state);

            if ("COMPLETED".equals(state)) {
                Map<String, String> userData = pendingUsers.remove(merchantOrderId);
                System.out.println("📦 userData found: " + (userData != null));

                if (userData != null) {
                    try {
                        User user = new User();
                        user.setName(userData.get("name"));
                        user.setPhoneNumber(userData.get("phoneNumber"));
                        user.setEmail(userData.get("email"));
                        user.setPassword(passwordEncoder.encode(userData.get("password")));
                        user.setPlateNumber(userData.get("plateNumber"));
                        user.setRCNumber(userData.get("rcNumber"));
                        user.setBalance(Double.parseDouble(userData.get("amount")));
                        user.setBranch(null);
                        userRepo.save(user);
                        System.out.println("✅ User saved: " + userData.get("email"));
                    } catch (Exception saveEx) {
                        saveEx.printStackTrace();
                        System.out.println("❌ Save failed: " + saveEx.getMessage());
                        return ResponseEntity.status(302)
                                .header("Location", frontendUrl + "/registrationFailed")
                                .build();
                    }
                } else {
                    // ✅ userData is null but payment is COMPLETED
                    // means first call already saved the user — still redirect to success
                    System.out.println("⚠️ userData null but COMPLETED — user already saved by previous call");
                }

                // ✅ Always go to success if payment is COMPLETED
                return ResponseEntity.status(302)
                        .header("Location", frontendUrl + "/registrationSuccess")
                        .build();

            } else {
                pendingUsers.remove(merchantOrderId);
                System.out.println("❌ Payment not completed: " + state);
                return ResponseEntity.status(302)
                        .header("Location", frontendUrl + "/registrationFailed")
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/registrationFailed")
                    .build();
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam(required = false) String merchantOrderId,
            @RequestParam(required = false) String transactionId) {
        try {
            if (merchantOrderId == null || merchantOrderId.isEmpty()) {
                return ResponseEntity.status(302)
                        .header("Location", frontendUrl + "/registrationFailed") // ✅ fixed
                        .build();
            }

            OrderStatusResponse statusResponse =
                    phonePeClient.getOrderStatus(merchantOrderId);
            String state = statusResponse.getState();

            if ("COMPLETED".equals(state)) {
                Map<String, String> userData = pendingUsers.remove(merchantOrderId);
                if (userData != null) {
                    User user = new User();
                    user.setName(userData.get("name"));
                    user.setPhoneNumber(userData.get("phoneNumber"));
                    user.setEmail(userData.get("email"));
                    user.setPassword(passwordEncoder.encode(userData.get("password")));
                    user.setPlateNumber(userData.get("plateNumber"));
                    user.setBalance(Double.parseDouble(userData.get("amount")));
                    user.setRCNumber(userData.get("rcNumber"));
                    user.setBranch(null);
                    userRepo.save(user);
                    System.out.println("✅ User saved: " + userData.get("email"));
                }
                return ResponseEntity.status(302)
                        .header("Location", frontendUrl + "/registrationSuccess") // ✅ fixed
                        .build();
            } else {
                pendingUsers.remove(merchantOrderId);
                return ResponseEntity.status(302)
                        .header("Location", frontendUrl + "/registrationFailed") // ✅ fixed
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(302)
                    .header("Location", frontendUrl + "/registrationFailed") // ✅ fixed
                    .build();
        }
    }

    @GetMapping("/status/{merchantOrderId}")
    public ResponseEntity<Map<String, Object>> checkStatus(
            @PathVariable String merchantOrderId) {
        try {
            OrderStatusResponse status =
                    phonePeClient.getOrderStatus(merchantOrderId);
            return ResponseEntity.ok(Map.of(
                "state",           status.getState(),
                "merchantOrderId", status.getMerchantOrderId(),
                "amount",          status.getAmount()
            ));
        } catch (PhonePeException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ MOCK — remove in production
    @PostMapping("/mock-register")
    public ResponseEntity<Map<String, Object>> mockRegister(
            @RequestBody Map<String, String> request) {
        try {
            String name        = request.get("name");
            String phone       = request.get("phoneNumber");
            String email       = request.get("email");
            String password    = request.get("password");
            String plateNumber = request.get("plateNumber");
            String rcNumber    = request.get("rcNumber");
            String amountStr   = request.get("amount");

            double amount = Double.parseDouble(amountStr);
            if (amount < 200) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Minimum amount is ₹200"));
            }

            if (userRepo.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already registered"));
            }

            User user = new User();
            user.setName(name);
            user.setPhoneNumber(phone);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setPlateNumber(plateNumber);
            user.setRCNumber(rcNumber);
            user.setBalance(amount);
            user.setBranch(null);
            userRepo.save(user);

            return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "name",    name,
                "email",   email
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
}