package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RestController;
import com.example.demo.entity.User;
import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VehicleRepository;
import com.example.demo.service.UserService;

@RestController
public class UserController 
{
	@Autowired
	private UserService service;
	
	@Autowired
	private UserRepository repo;
	
	@Autowired
	private VehicleRepository vehicleLogRepo;
	
    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;
    
	@PostMapping("/register")
	private User registerUser(@RequestBody User user)
	{
		return service.registerUser(user);
	}
	
	// ✅ Fixed — always returns JSON
	@PostMapping("/userLogin")
	public ResponseEntity<?> userLogin(@RequestBody Map<String, String> loginData) {
	    try {
	        String name     = loginData.get("name");
	        String password = loginData.get("password");
	        User user = service.userLogin(name, password);
	        return ResponseEntity.ok(user);
	    } catch (Exception e) {
	        return ResponseEntity.status(401)
	                .body(Map.of("error", "Invalid username or password"));
	    }
	}
	
	// ✅ Send OTP
	@PostMapping("/api/user/forgot-password/send-otp")
	public ResponseEntity<Map<String, String>> sendOtp(
	        @RequestBody Map<String, String> request) {
	    try {
	        String email = request.get("email");
	        service.sendOtp(email);

	        // ✅ Return masked email for confirmation
	        String masked = maskEmail(email);
	        return ResponseEntity.ok(Map.of(
	            "message", "OTP sent successfully",
	            "maskedEmail", masked
	        ));
	    } catch (Exception e) {
	        return ResponseEntity.status(400)
	                .body(Map.of("error", e.getMessage()));
	    }
	}

	private String maskEmail(String email) {
	    int atIndex = email.indexOf("@");
	    if (atIndex <= 2) return email;
	    String local  = email.substring(0, atIndex);
	    String domain = email.substring(atIndex);
	    String masked = local.substring(0, 2)
	            + "*".repeat(local.length() - 2)
	            + domain;
	    return masked;
	}

	// ✅ Verify OTP
	@PostMapping("/api/user/forgot-password/verify-otp")
	public ResponseEntity<Map<String, Object>> verifyOtp(
	        @RequestBody Map<String, String> request) {
	    boolean valid = service.verifyOtp(
	            request.get("email"), request.get("otp"));
	    if (valid) {
	        return ResponseEntity.ok(Map.of("valid", true));
	    } else {
	        return ResponseEntity.status(400)
	                .body(Map.of("valid", false, "error", "Invalid or expired OTP"));
	    }
	}

	// ✅ Reset password
	@PostMapping("/api/user/forgot-password/reset")
	public ResponseEntity<Map<String, String>> resetPassword(
	        @RequestBody Map<String, String> request) {
	    try {
	        service.resetPassword(
	                request.get("email"),
	                request.get("otp"),
	                request.get("newPassword"));
	        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
	    } catch (Exception e) {
	        return ResponseEntity.status(400)
	                .body(Map.of("error", e.getMessage()));
	    }
	}
	
	// ✅ Get transaction history for logged-in user
	@GetMapping("/user/transactions")
	public ResponseEntity<List<VehicleLog>> getTransactions(
	        @AuthenticationPrincipal UserDetails userDetails) {
	    User user = repo.findUserByName(userDetails.getUsername())
	            .orElseThrow(() -> new RuntimeException("User not found"));
	    List<VehicleLog> logs = vehicleLogRepo.findByPlateNumberAndInsideFalse(
	            user.getPlateNumber());
	    return ResponseEntity.ok(logs);
	}

	// ✅ Initiate top-up payment (reuse PhonePe flow)
	/*@PostMapping("/user/topup/initiate")
	public ResponseEntity<Map<String, Object>> initiateTopUp(
	        @AuthenticationPrincipal UserDetails userDetails,
	        @RequestBody Map<String, String> request) {
	    try {
	        User user = repo.findUserByName(userDetails.getUsername())
	                .orElseThrow(() -> new RuntimeException("User not found"));

	        String amountStr = request.get("amount");
	        double amount    = Double.parseDouble(amountStr);

	        if (amount < 200) {
	            return ResponseEntity.badRequest()
	                    .body(Map.of("error", "Minimum top-up is ₹200"));
	        }

	        String merchantOrderId = "TOPUP_" + UUID.randomUUID()
	                .toString().replace("-", "");

	        // store pending topup
	        pendingTopUps.put(merchantOrderId, Map.of(
	            "userId",   String.valueOf(user.getId()),
	            "amount",   amountStr
	        ));

	        long amountInPaise = (long)(amount * 100);
	        String redirectUrl = backendUrl + "/user/topup/callback";

	        StandardCheckoutPayRequest payRequest = StandardCheckoutPayRequest.builder()
	            .merchantOrderId(merchantOrderId)
	            .amount(amountInPaise)
	            .redirectUrl(redirectUrl)
	            .build();

	        StandardCheckoutPayResponse payResponse = phonePeClient.pay(payRequest);

	        return ResponseEntity.ok(Map.of(
	            "paymentUrl",      payResponse.getRedirectUrl(),
	            "merchantOrderId", merchantOrderId
	        ));

	    } catch (Exception e) {
	        return ResponseEntity.status(500)
	                .body(Map.of("error", e.getMessage()));
	    }
	}

	// ✅ Callback after top-up payment
	@GetMapping("/user/topup/callback")
	public ResponseEntity<Void> topUpCallback(
	        @RequestParam(required = false) String merchantOrderId) {
	    try {
	        if (merchantOrderId == null) {
	            return ResponseEntity.status(302)
	                    .header("Location", frontendUrl + "/userDashboard?topup=failed")
	                    .build();
	        }

	        OrderStatusResponse status = phonePeClient.getOrderStatus(merchantOrderId);

	        if ("COMPLETED".equals(status.getState())) {
	            Map<String, String> pending = pendingTopUps.remove(merchantOrderId);
	            if (pending != null) {
	                Long userId = Long.parseLong(pending.get("userId"));
	                double amount = Double.parseDouble(pending.get("amount"));
	                User user = repo.findById(userId)
	                        .orElseThrow(() -> new RuntimeException("User not found"));
	                user.setBalance(user.getBalance() + amount);
	                repo.save(user);
	            }
	            return ResponseEntity.status(302)
	                    .header("Location", frontendUrl + "/userDashboard?topup=success")
	                    .build();
	        } else {
	            pendingTopUps.remove(merchantOrderId);
	            return ResponseEntity.status(302)
	                    .header("Location", frontendUrl + "/userDashboard?topup=failed")
	                    .build();
	        }
	    } catch (Exception e) {
	        return ResponseEntity.status(302)
	                .header("Location", frontendUrl + "/userDashboard?topup=failed")
	                .build();
	    }
	}*/

}
