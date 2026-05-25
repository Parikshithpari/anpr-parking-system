package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.example.demo.entity.Branch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.example.demo.entity.BranchUser;
import com.example.demo.entity.User;
import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VehicleRepository;

@Service
public class VehicleLogService
{
    @Autowired
    private VehicleRepository repo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BranchRepository branchRepo; 

    public List<VehicleLog> getLogs(BranchUser user) 
    {
        if (user.getBranches() == null || user.getBranches().isEmpty()) 
        {
            return new ArrayList<>();
        }
        return repo.findByBranchId(user.getBranches().get(0).getId());
    }
    
    public List<VehicleLog> getLogsByBranchId(Long branchId) 
    {
        return repo.findByBranchId(branchId);
    }

    public VehicleLog logEntry(VehicleLog log, Authentication auth) 
    {
        BranchUser user = (BranchUser) auth.getPrincipal();
        if (!user.getBranches().isEmpty()) {
            log.setBranch(user.getBranches().get(0));
        }
        log.setEntryTime(LocalDateTime.now());
        log.setInside(true);
        return repo.save(log);
    }

    public VehicleLog logExit(Long id)
    {
        VehicleLog log = repo.findById(id).orElseThrow();
        // ✅ get branch price
        double branchPrice = getBranchPrice(log.getBranch());
        log.setExitTime(LocalDateTime.now());
        log.setInside(false);
        log.calculatePrice(branchPrice);
        return repo.save(log);
    }

    public VehicleLog logExitByPlate(String plateNumber)
    {
        VehicleLog log = repo.findByPlateNumberAndInsideTrue(plateNumber)
                             .orElseThrow(() -> new RuntimeException("Vehicle not inside"));
        // ✅ get branch price
        double branchPrice = getBranchPrice(log.getBranch());
        log.setExitTime(LocalDateTime.now());
        log.setInside(false);
        log.calculatePrice(branchPrice);
        return repo.save(log);
    }

    private double getBranchPrice(Branch branch)
    {
        if (branch == null) return 2.0;

        Branch fresh = branchRepo.findById(branch.getId()).orElse(branch);
        return fresh.getPricePerMinute() != null ? fresh.getPricePerMinute() : 2.0;
    }

    private boolean fuzzyMatch(String ocr, String registered) {
        if (ocr == null || registered == null) return false;
        // ✅ Normalize to uppercase for comparison
        String ocrUp = ocr.toUpperCase().replaceAll("\\s+", "");
        String regUp = registered.toUpperCase().replaceAll("\\s+", "");
        if (ocrUp.equals(regUp)) return true;
        if (Math.abs(ocrUp.length() - regUp.length()) > 1) return false;
        int diff = 0;
        int len  = Math.min(ocrUp.length(), regUp.length());
        for (int i = 0; i < len; i++) {
            if (ocrUp.charAt(i) != regUp.charAt(i)) diff++;
            if (diff > 1) return false;
        }
        return true;
    }

    private void deductBalance(User vehicleUser, double price) {
        // ✅ Always fetch fresh from DB by ID to avoid stale state
        User freshUser = userRepo.findById(vehicleUser.getId())
                .orElse(vehicleUser);
     
        double current = freshUser.getBalance() != null ? freshUser.getBalance() : 0.0;
     
        if (price <= 0) {
            System.out.println("⚠️ Price is 0 or negative — skipping deduction");
            return;
        }
     
        if (current >= price) {
            freshUser.setBalance(Math.round((current - price) * 100.0) / 100.0);
        } else {
            System.out.println("⚠️ Insufficient balance for: " + freshUser.getPlateNumber()
                + " | Required: " + price + " | Available: " + current);
            freshUser.setBalance(0.0);
        }
     
        userRepo.save(freshUser);
        System.out.println("✅ Balance deducted for " + freshUser.getPlateNumber()
            + " | Old: " + current
            + " | Deducted: " + price
            + " | New: " + freshUser.getBalance());
    }

    public VehicleLog handleDetection(String plate, String cameraId, BranchUser user) {
        Branch currentBranch = user.getBranches().isEmpty()
                ? null : user.getBranches().get(0);
     
        double branchPrice = getBranchPrice(currentBranch);
     
        // ✅ Search ALL users (not just branch-specific) for plate match
        // This handles users registered without a branch
        List<User> allUsers = userRepo.findAll();
        Optional<User> registered = allUsers.stream()
                .filter(u -> fuzzyMatch(plate, u.getPlateNumber()))
                .findFirst();
     
        if (registered.isPresent()) {
            Optional<VehicleLog> existing = repo.findByPlateNumberAndInsideTrue(plate);
     
            if (existing.isPresent()) {
                // Registered vehicle exiting
                VehicleLog log = existing.get();
     
                // ✅ Use the branch price of the branch where vehicle is parked
                double exitBranchPrice = getBranchPrice(log.getBranch());
     
                log.setExitTime(LocalDateTime.now());
                log.setInside(false);
                log.calculatePrice(exitBranchPrice);
     
                double price = log.getPrice() != null ? log.getPrice() : 0.0;
                deductBalance(registered.get(), price);
     
                System.out.println("✅ Exit: " + plate
                    + " | Price: Rs." + price
                    + " | Rate: Rs." + exitBranchPrice + "/min"
                    + " | Branch: " + (log.getBranch() != null ? log.getBranch().getBranchName() : "unknown"));
     
                return repo.save(log);
            } else {
                // Registered vehicle entering
                VehicleLog log = new VehicleLog();
                log.setPlateNumber(plate);
                log.setBranch(currentBranch);
                log.setEntryTime(LocalDateTime.now());
                log.setInside(true);
                System.out.println("✅ Entry: " + plate);
                return repo.save(log);
            }
        } else {
            System.out.println("ℹ️ Unregistered vehicle: " + plate);
            Optional<VehicleLog> existing = repo.findByPlateNumberAndInsideTrue(plate);
     
            if (existing.isPresent()) {
                VehicleLog log = existing.get();
                double exitBranchPrice = getBranchPrice(log.getBranch());
                log.setExitTime(LocalDateTime.now());
                log.setInside(false);
                log.calculatePrice(exitBranchPrice);
                return repo.save(log);
            } else {
                VehicleLog log = new VehicleLog();
                log.setPlateNumber(plate);
                log.setBranch(currentBranch);
                log.setEntryTime(LocalDateTime.now());
                log.setInside(true);
                return repo.save(log);
            }
        }
    }
    
    public List<VehicleLog> logsForSuperAdmin()
    {
        return repo.findAll();
    }
}