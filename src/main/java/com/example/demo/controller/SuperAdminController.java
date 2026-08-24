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
import com.example.demo.service.RTOInsuranceService;
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
    private SuperAdminRepository superAdminRepo;  
    
    @Autowired
    private RTOInsuranceService rtoInsuranceService;

    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${anpr.api.key}")
    private String anprApiKey;
   

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
            List<com.example.demo.entity.User> users = userRepo.findAll();
            Optional<com.example.demo.entity.User> found = users.stream()
                    .filter(u -> plateNumber.equalsIgnoreCase(u.getPlateNumber()))
                    .findFirst();

            if (found.isEmpty()) {
                return ResponseEntity.ok(Map.of("found", false));
            }

            com.example.demo.entity.User u = found.get();
            Map<String, Object> map = new HashMap<>();
            map.put("found",            true);
            map.put("id",               u.getId());
            map.put("name",             u.getName());
            map.put("email",            u.getEmail());
            map.put("phoneNumber",      u.getPhoneNumber());
            map.put("plateNumber",      u.getPlateNumber());
            map.put("balance",          u.getBalance());
            map.put("rcNumber",         u.getRCNumber());
            map.put("insuranceStatus",  u.getInsuranceStatus() != null
                                        ? u.getInsuranceStatus() : "PENDING");
            map.put("insuranceCompany", u.getInsuranceCompany());
            map.put("insuranceExpiry",  u.getInsuranceExpiry());
            map.put("dateOfBirth", u.getDateOfBirth());
            return ResponseEntity.ok(map);
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
            String rtspUrl    = request.get("rtspUrl");
     
            Branch branch = new Branch();
            branch.setBranchName(branchName);
            branch.setLocation(location);
            branch.setUserName(userName);
            branch.setPassword(password);                    // ✅ NEW — store plain for Python
            branch.setPassword(passwordEncoder.encode(password)); // existing — BCrypt for security
            branch.setPricePerMinute(2.0);
            branch.setRtspUrl(rtspUrl);
            Branch savedBranch = branchRepo.save(branch);
     
            BranchUser user = new BranchUser();
            user.setBranchName(branchName);
            user.setLocation(location);
            user.setUserName(userName);
            user.setPassword(passwordEncoder.encode(password));   // unchanged
            user.setBranches(new ArrayList<>(List.of(savedBranch)));
            BranchUser saved = branchUserRepo.save(user);
     
            cacheService.evictBranchCaches();
     
            // Response — identical to original
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
        try {
            BranchUser user = branchUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
     
            String tempPassword = generateTempPassword();
     
            // Update BranchUser password
            user.setPassword(passwordEncoder.encode(tempPassword));
            branchUserRepo.save(user);
     
            // ✅ Also update Branch plainPassword so Python still works after reset
            List<Branch> userBranches = branchRepo.findAll().stream()
                .filter(b -> b.getUserName() != null
                          && b.getUserName().equals(user.getUsername()))
                .collect(java.util.stream.Collectors.toList());
     
            for (Branch b : userBranches) {
                b.setPassword(tempPassword);
                b.setPassword(passwordEncoder.encode(tempPassword));
                branchRepo.save(b);
            }
     
            return ResponseEntity.ok(Map.of("temporaryPassword", tempPassword));
     
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/super-admin/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<com.example.demo.entity.User> users = userRepo.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",               u.getId());
            map.put("name",             u.getName());
            map.put("email",            u.getEmail());
            map.put("phoneNumber",      u.getPhoneNumber());
            map.put("plateNumber",      u.getPlateNumber());
            map.put("rcNumber",         u.getRCNumber());
            map.put("balance",          u.getBalance());
            map.put("insuranceStatus",  u.getInsuranceStatus() != null
                                        ? u.getInsuranceStatus() : "PENDING");
            map.put("insuranceCompany", u.getInsuranceCompany());
            map.put("insuranceExpiry",  u.getInsuranceExpiry());
            map.put("insuranceNote",    u.getInsuranceNote());
            map.put("dateOfBirth", u.getDateOfBirth());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ✅ Update insurance status for a user
    @PutMapping("/super-admin/users/{id}/insurance")
    public ResponseEntity<Map<String, Object>> updateInsurance(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            com.example.demo.entity.User user = userRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String status  = request.get("insuranceStatus");
            String company = request.get("insuranceCompany");
            String expiry  = request.get("insuranceExpiry");
            String note    = request.get("insuranceNote");

            if (status  != null) user.setInsuranceStatus(status);
            if (company != null) user.setInsuranceCompany(company);
            if (expiry  != null) user.setInsuranceExpiry(expiry);
            if (note    != null) user.setInsuranceNote(note);

            userRepo.save(user);

            return ResponseEntity.ok(Map.of(
                "message",          "Insurance status updated",
                "insuranceStatus",  user.getInsuranceStatus(),
                "insuranceCompany", user.getInsuranceCompany() != null ? user.getInsuranceCompany() : "",
                "insuranceExpiry",  user.getInsuranceExpiry()  != null ? user.getInsuranceExpiry()  : "",
                "insuranceNote",    user.getInsuranceNote()    != null ? user.getInsuranceNote()    : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    } 
    
    @PostMapping("/super-admin/users/{id}/fetch-insurance")
    public ResponseEntity<Map<String, Object>> fetchInsuranceFromAPI(@PathVariable Long id) {
      try {
         com.example.demo.entity.User user = userRepo.findById(id)
                  .orElseThrow(() -> new RuntimeException("User not found"));
 
          if (user.getRCNumber() == null || user.getRCNumber().isBlank()) {
              return ResponseEntity.badRequest().body(Map.of("error", "No RC number on file"));
         }
 
         Map<String, Object> apiResult = rtoInsuranceService.fetchInsuranceDetails(user.getRCNumber());
 
         if (Boolean.TRUE.equals(apiResult.get("success"))) {
              user.setInsuranceCompany((String) apiResult.get("insuranceCompany"));
              user.setInsuranceExpiry((String) apiResult.get("insuranceExpiry"));
              user.setInsuranceStatus((String) apiResult.get("insuranceStatus"));
              userRepo.save(user);
          }
 
          return ResponseEntity.ok(apiResult);
 
      } catch (Exception e) {
          return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
      }
}
    
 // ✅ Python polls this to get all active camera configs
 // No auth needed — Python uses its own API key
    @GetMapping("/api/anpr/camera-configs")
    public ResponseEntity<?> getCameraConfigs(
            @RequestHeader(value = "X-ANPR-Key", required = false) String apiKey) {
     
        if (apiKey == null || !anprApiKey.equals(apiKey.trim())) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid ANPR API key"));
        }
     
        List<Branch> branches = branchRepo.findAll().stream()
                .filter(b -> b.getRtspUrl() != null && !b.getRtspUrl().isBlank())
                .collect(java.util.stream.Collectors.toList());
     
        List<Map<String, Object>> result = branches.stream().map(b -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("branchId",      b.getId());
            map.put("branchName",    b.getBranchName());
            map.put("rtspUrl",       b.getRtspUrl());
            map.put("cameraId",      "CAM-" + b.getId());
            map.put("username",      b.getUserName());
            map.put("password",      b.getPassword()); // ✅ plain text — Python uses this to login
            return map;
        }).collect(java.util.stream.Collectors.toList());
     
        return ResponseEntity.ok(result);
    }

}