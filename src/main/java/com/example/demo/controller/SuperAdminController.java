package com.example.demo.controller;

import com.example.demo.entity.Branch;
import com.example.demo.entity.BranchUser;
import com.example.demo.entity.SuperAdmin;
import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.BranchUserRepository;
import com.example.demo.repository.SuperAdminRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.SuperAdminCacheService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.service.VehicleLogService;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SuperAdminController {

    @Autowired 
    private VehicleLogService vehicleLogService;
    
    @Autowired 
    private JwtUtil jwtUtil;
    
    @Autowired 
    private SuperAdminService superAdminDetailService;
    
    @Autowired 
    private PasswordEncoder passwordEncoder;
    
    @Autowired 
    private BranchUserRepository branchUserRepo;
    
    @Autowired 
    private BranchRepository branchRepo;
    
    @Autowired 
    private UserRepository userRepo;
    
    @Autowired
    private SuperAdminService superAdminService;
    
    @Autowired 
    private SuperAdminCacheService cacheService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SuperAdminRepository superAdminRepo;  // ✅ was missing

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ✅ OTP store — in memory
    private final Map<String, String[]> superAdminOtpStore = new ConcurrentHashMap<>();

    // ✅ Step 1 — validate credentials, send OTP (only one login mapping now)
    @PostMapping("/super-admin/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            UserDetails superAdmin = superAdminDetailService.loadUserByUsername(username);
            if (!passwordEncoder.matches(password, superAdmin.getPassword())) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid credentials"));
            }

            SuperAdmin admin = superAdminRepo.findByUserName(username)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            // ✅ Guard — if email not set, skip OTP and just return token
            if (admin.getEmail() == null || admin.getEmail().isBlank()) {
                System.out.println("⚠️ No email set for super admin — skipping OTP");
                String token = jwtUtil.generateToken(username);
                return ResponseEntity.ok(Map.of(
                    "token",       token,
                    "role",        "SUPER_ADMIN",
                    "otpRequired", "false"
                ));
            }

            String otp    = String.valueOf((int)(Math.random() * 900000) + 100000);
            long   expiry = System.currentTimeMillis() + (5 * 60 * 1000);
            superAdminOtpStore.put(username, new String[]{otp, String.valueOf(expiry)});

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(admin.getEmail());
            helper.setSubject("PTag Super Admin — Login OTP");
            helper.setText(
                "<div style='font-family:Segoe UI,sans-serif;max-width:480px;" +
                "margin:0 auto;padding:32px;background:#faf4f2;" +
                "border-radius:16px;border:1px solid rgba(139,58,58,0.2)'>" +
                "<h2 style='color:#5C1F1F;margin:0 0 8px'>Super Admin Login OTP</h2>" +
                "<p style='color:#9a6060;font-size:14px;margin:0 0 24px'>" +
                "Use the OTP below to complete your login. Valid for 5 minutes.</p>" +
                "<div style='background:#5C1F1F;border-radius:12px;padding:20px;" +
                "text-align:center;letter-spacing:8px;font-size:32px;" +
                "font-weight:800;color:#ffffff'>" + otp + "</div>" +
                "<p style='color:#c09090;font-size:12px;margin:20px 0 0'>" +
                "If you did not attempt to login, secure your account immediately.</p></div>",
                true
            );
            mailSender.send(message);

            return ResponseEntity.ok(Map.of(
                "message",     "OTP sent to registered email",
                "otpRequired", "true",
                "username",    username
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/super-admin/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String otp      = request.get("otp");

            String[] stored = superAdminOtpStore.get(username);
            if (stored == null) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "OTP expired or not requested"));
            }

            long expiry = Long.parseLong(stored[1]);
            if (System.currentTimeMillis() > expiry) {
                superAdminOtpStore.remove(username);
                return ResponseEntity.status(400)
                        .body(Map.of("error", "OTP expired. Please login again."));
            }

            if (!stored[0].equals(otp)) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Invalid OTP"));
            }

            // ✅ OTP valid — generate JWT and clear OTP
            superAdminOtpStore.remove(username);
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of(
                "token", token,
                "role",  "SUPER_ADMIN"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Verification failed"));
        }
    }

    // ── Logs ──────────────────────────────────────────────────────────────
    @GetMapping("/super-admin/logs")
    public ResponseEntity<List<VehicleLog>> getAllLogs() {
        return ResponseEntity.ok(vehicleLogService.logsForSuperAdmin());
    }

    // ── Branch users ──────────────────────────────────────────────────────
    @GetMapping("/super-admin/branch-users")
    public ResponseEntity<List<Map<String, Object>>> getBranchUsers() {
        return ResponseEntity.ok(cacheService.getBranchUsers());
    }

    // ── All branches ──────────────────────────────────────────────────────
    @GetMapping("/super-admin/all-branches")
    public ResponseEntity<List<Map<String, Object>>> getAllBranches() {
        return ResponseEntity.ok(superAdminService.getAllBranches());
    }

    // ── User by plate ─────────────────────────────────────────────────────
    @GetMapping("/super-admin/user-by-plate/{plateNumber}")
    public ResponseEntity<Map<String, Object>> getUserByPlate(
            @PathVariable String plateNumber) {
        try {
            return ResponseEntity.ok(cacheService.getUserByPlate(plateNumber));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Branch password ───────────────────────────────────────────────────
    @GetMapping("/super-admin/branch-users/{id}/password")
    public ResponseEntity<Map<String, Object>> getBranchPassword(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(cacheService.getBranchPassword(id));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Create branch user ────────────────────────────────────────────────
    @PostMapping("/super-admin/branch-users")
    public ResponseEntity<Map<String, Object>> createBranchUser(
            @RequestBody Map<String, String> request) {
        try {
            String branchName = request.get("branchName");
            String location   = request.get("location");
            String userName   = request.get("userName");
            String password   = request.get("password");

            Branch branch = new Branch();
            branch.setBranchName(branchName);
            branch.setLocation(location);
            branch.setUserName(userName);
            branch.setPassword(passwordEncoder.encode(password));
            branch.setPricePerMinute(2.0);
            Branch savedBranch = branchRepo.save(branch);

            BranchUser user = new BranchUser();
            user.setBranchName(branchName);
            user.setLocation(location);
            user.setUserName(userName);
            user.setPassword(passwordEncoder.encode(password));
            user.setBranches(new ArrayList<>(List.of(savedBranch)));
            BranchUser saved = branchUserRepo.save(user);

            cacheService.evictBranchCaches();

            Map<String, Object> resp = new HashMap<>();
            resp.put("id",         saved.getId());
            resp.put("branchName", saved.getBranchName());
            resp.put("location",   saved.getLocation());
            resp.put("userName",   saved.getUsername());
            resp.put("branches",   List.of(Map.of(
                "id",         savedBranch.getId(),
                "branchName", savedBranch.getBranchName(),
                "location",   savedBranch.getLocation()
            )));
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create: " + e.getMessage()));
        }
    }

    // ── Assign branches ───────────────────────────────────────────────────
    @PutMapping("/super-admin/branch-users/{id}/assign-branches")
    public ResponseEntity<Map<String, Object>> assignBranches(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {
        try {
            BranchUser user = branchUserRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Branch user not found"));

            List<Long> branchIds = request.get("branchIds");
            List<Branch> branches = branchRepo.findAllById(branchIds);
            user.setBranches(branches);
            branchUserRepo.save(user);

            cacheService.evictBranchCaches();

            return ResponseEntity.ok(Map.of(
                "message", "Branches assigned successfully",
                "count",   branches.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to assign: " + e.getMessage()));
        }
    }

    // ── Delete branch user ────────────────────────────────────────────────
    @DeleteMapping("/super-admin/branch-users/{id}")
    public ResponseEntity<Map<String, String>> deleteBranchUser(
            @PathVariable Long id) {
        try {
            branchUserRepo.deleteById(id);
            cacheService.evictBranchCaches();
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to delete: " + e.getMessage()));
        }
    }

    // ── Download config ───────────────────────────────────────────────────
    @GetMapping("/super-admin/branch-users/{id}/download-config")
    public ResponseEntity<byte[]> downloadConfig(
            @PathVariable Long id,
            @RequestParam String backendUrl) {
        BranchUser user = branchUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch user not found"));

        String configContent =
            "[server]\n" +
            "backend_url = " + backendUrl + "/api/anpr/notify\n" +
            "login_url   = " + backendUrl + "/api/auth/login\n" +
            "username    = " + user.getUsername() + "\n" +
            "password    = ENTER_PLAIN_TEXT_PASSWORD_HERE\n\n" +
            "[camera]\n" +
            "index     = 0\n" +
            "camera_id = CAM-" + user.getId() + "\n\n" +
            "[settings]\n" +
            "cooldown       = 10\n" +
            "every_n_frames = 3\n" +
            "save_dir       = detections\n";

        byte[] bytes = configContent.getBytes();
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=config_" + user.getUsername() + ".ini")
                .header("Content-Type", "text/plain")
                .body(bytes);
    }

    // ── Reset password ────────────────────────────────────────────────────
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @PostMapping("/super-admin/branch-users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        System.out.println("🔑 Reset password called for user id: " + id);
        try {
            BranchUser user = branchUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String tempPassword = generateTempPassword();
            user.setPassword(passwordEncoder.encode(tempPassword));
            branchUserRepo.save(user);

            System.out.println("✅ Password reset for user: " + user.getUsername());

            return ResponseEntity.ok(Map.of("temporaryPassword", tempPassword));

        } catch (Exception e) {
            System.out.println("❌ Reset failed: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}