package com.example.demo.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.DetectionRequest;
import com.example.demo.entity.Branch;
import com.example.demo.entity.BranchUser;
import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.BranchRepository;
import com.example.demo.service.VehicleLogService;
import org.springframework.transaction.annotation.Transactional;

@RestController
@Transactional
public class VehicleLogController 
{

    @Autowired
    private VehicleLogService service;

    @Autowired
    private BranchRepository branchRepo;
   

    @CrossOrigin(origins = "https://gconnectt.com")
    @GetMapping("/getLogs")
    public List<VehicleLog> getLogs(
            @AuthenticationPrincipal BranchUser user,
            @RequestParam(required = false) Long branchId) 
    {
        if (branchId != null) 
        {
            return service.getLogsByBranchId(branchId);
        }
        if (user.getBranches() == null || user.getBranches().isEmpty()) 
        {
            return new ArrayList<>();
        }
        return service.getLogsByBranchId(user.getBranches().get(0).getId());
    }

    @PostMapping("/log-vehicleEntry")
    public VehicleLog logEntry(@RequestBody VehicleLog log, Authentication auth) {
        return service.logEntry(log, auth);
    }

    @PostMapping("/log-vehicleExit/{id}")
    public VehicleLog logExit(@PathVariable Long id) {
        return service.logExit(id);
    }

    @PostMapping("/log-vehicleExitByPlate/{plateNumber}")
    public VehicleLog logExitByPlate(@PathVariable String plateNumber) {
        return service.logExitByPlate(plateNumber);
    }

    @PostMapping("/api/vehicle/plate")
    public VehicleLog detectPlate(@RequestBody DetectionRequest request,
                                  @AuthenticationPrincipal BranchUser user) {
        return service.handleDetection(request.getPlate(), request.getCameraId(), user);
    }

    // ✅ Get all assigned branches for logged in branch user
    @GetMapping("/branch/my-branches")
    public ResponseEntity<List<Map<String, Object>>> getMyBranches(
            @AuthenticationPrincipal BranchUser user) {
        List<Map<String, Object>> result = user.getBranches().stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",              b.getId());
            map.put("branchName",      b.getBranchName());
            map.put("location",        b.getLocation());
            map.put("pricePerMinute",  b.getPricePerMinute() != null
                                       ? b.getPricePerMinute() : 2.0);
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ✅ Update price for a specific branch
    @PutMapping("/branch/price/{branchId}")
    public ResponseEntity<Map<String, Object>> updatePrice(
            @PathVariable Long branchId,
            @RequestBody Map<String, Object> request,   // ← change Double to Object to handle mixed types
            @AuthenticationPrincipal BranchUser user) {
        try {
            boolean owns = user.getBranches().stream()
                    .anyMatch(b -> b.getId().equals(branchId));
            if (!owns) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Not authorized for this branch"));
            }

            Branch branch = branchRepo.findById(branchId)
                    .orElseThrow(() -> new RuntimeException("Branch not found"));

            // ✅ Existing behavior — unchanged
            if (request.containsKey("pricePerMinute")) {
                branch.setPricePerMinute(((Number) request.get("pricePerMinute")).doubleValue());
            }

            // ✅ NEW — only set special price if both fields are provided
            if (request.containsKey("specialPrice") && request.containsKey("specialPriceUntil")) {
                branch.setSpecialPrice(((Number) request.get("specialPrice")).doubleValue());
                branch.setSpecialPriceUntil(LocalDate.parse((String) request.get("specialPriceUntil")));
            }

            // ✅ NEW — allow clearing special price explicitly
            if (request.containsKey("clearSpecialPrice") && Boolean.TRUE.equals(request.get("clearSpecialPrice"))) {
                branch.setSpecialPrice(null);
                branch.setSpecialPriceUntil(null);
            }

            branchRepo.save(branch);

            // ✅ Existing response + new fields appended
            return ResponseEntity.ok(Map.of(
                "message",           "Price updated successfully",
                "pricePerMinute",    branch.getPricePerMinute(),       // existing
                "specialPrice",      branch.getSpecialPrice() != null ? branch.getSpecialPrice() : "",
                "specialPriceUntil", branch.getSpecialPriceUntil() != null ? branch.getSpecialPriceUntil() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update price: " + e.getMessage()));
        }
    }

    // ✅ Get price for a specific branch
    @GetMapping("/branch/price/{branchId}")
    public ResponseEntity<Map<String, Object>> getPrice(
            @PathVariable Long branchId,
            @AuthenticationPrincipal BranchUser user) {

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        // ✅ Existing behavior — unchanged
        Double normalPrice = branch.getPricePerMinute() != null ? branch.getPricePerMinute() : 2.0;

        // ✅ NEW — check if special price is still active
        boolean specialActive = branch.getSpecialPrice() != null
                && branch.getSpecialPriceUntil() != null
                && !LocalDate.now().isAfter(branch.getSpecialPriceUntil());

        return ResponseEntity.ok(Map.of(
            "pricePerMinute",    normalPrice,                                           // existing
            "effectivePrice",    specialActive ? branch.getSpecialPrice() : normalPrice, // NEW — use this for billing
            "specialPrice",      branch.getSpecialPrice() != null ? branch.getSpecialPrice() : "",  // NEW
            "specialPriceUntil", branch.getSpecialPriceUntil() != null ? branch.getSpecialPriceUntil() : "", // NEW
            "specialActive",     specialActive                                           // NEW
        ));
    }
    
}