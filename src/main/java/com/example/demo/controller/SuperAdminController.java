package com.example.demo.controller;

import com.example.demo.entity.Branch;
import com.example.demo.entity.BranchUser;
import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.BranchUserRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.SuperAdminCacheService;
import com.example.demo.service.SuperAdminService;
import com.example.demo.service.VehicleLogService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
    private SuperAdminCacheService cacheService; 


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
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token, "role", "SUPER_ADMIN"));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    // ── Logs (not cached — changes every entry/exit) ───────────────────────
    @GetMapping("/super-admin/logs")
    public ResponseEntity<List<VehicleLog>> getAllLogs() {
        return ResponseEntity.ok(vehicleLogService.logsForSuperAdmin());
    }

    // ── Branch users → served from Redis via cacheService ─────────────────
    @GetMapping("/super-admin/branch-users")
    public ResponseEntity<List<Map<String, Object>>> getBranchUsers() {
        return ResponseEntity.ok(cacheService.getBranchUsers());
    }

    // ── All branches → served from Redis via cacheService ─────────────────
    @GetMapping("/super-admin/all-branches")
    public ResponseEntity<List<Map<String, Object>>> getAllBranches() {
        return ResponseEntity.ok(cacheService.getAllBranches());
    }

    // ── User by plate → served from Redis via cacheService ────────────────
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

    // ── Branch password → served from Redis via cacheService ──────────────
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

    // ── Create branch user + evict cache ──────────────────────────────────
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

            cacheService.evictBranchCaches();  // ← bust stale cache

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

    // ── Assign branches + evict cache ──────────────────────────────────────
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

            cacheService.evictBranchCaches();  // ← bust stale cache

            return ResponseEntity.ok(Map.of(
                "message", "Branches assigned successfully",
                "count",   branches.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to assign: " + e.getMessage()));
        }
    }

    // ── Delete branch user + evict cache ──────────────────────────────────
    @DeleteMapping("/super-admin/branch-users/{id}")
    public ResponseEntity<Map<String, String>> deleteBranchUser(
            @PathVariable Long id) {
        try {
            branchUserRepo.deleteById(id);
            cacheService.evictBranchCaches();  // ← bust stale cache
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to delete: " + e.getMessage()));
        }
    }

    // ── Download config (no caching needed) ───────────────────────────────
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
}