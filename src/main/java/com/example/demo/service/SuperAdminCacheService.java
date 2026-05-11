package com.example.demo.service;

import com.example.demo.entity.Branch;
import com.example.demo.entity.BranchUser;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.BranchUserRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperAdminCacheService 
{

    @Autowired
    private BranchUserRepository branchUserRepo;

    @Autowired
    private BranchRepository branchRepo;

    @Autowired
    private UserRepository userRepo;

    // ── Hover card: cache per plate number ──
    @Cacheable(value = "userByPlate", key = "#plateNumber.toUpperCase()")
    public Map<String, Object> getUserByPlate(String plateNumber) {
        Optional<com.example.demo.entity.User> found = userRepo.findAll().stream()
                .filter(u -> plateNumber.equalsIgnoreCase(u.getPlateNumber()))
                .findFirst();

        if (found.isEmpty()) {
            return Map.of("found", false);
        }

        com.example.demo.entity.User u = found.get();
        Map<String, Object> map = new HashMap<>();
        map.put("found",       true);
        map.put("name",        u.getName());
        map.put("email",       u.getEmail());
        map.put("phoneNumber", u.getPhoneNumber());
        map.put("plateNumber", u.getPlateNumber());
        map.put("balance",     u.getBalance());
        return map;
    }

    // ── Branch users list ──
    @Cacheable(value = "branchUsers", key = "'all'")
    public List<Map<String, Object>> getBranchUsers() {
        return branchUserRepo.findAll().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",         u.getId());
            map.put("branchName", u.getBranchName());
            map.put("location",   u.getLocation());
            map.put("userName",   u.getUsername());

            List<Map<String, Object>> branchList = u.getBranches().stream().map(b -> {
                Map<String, Object> bMap = new HashMap<>();
                bMap.put("id",         b.getId());
                bMap.put("branchName", b.getBranchName());
                bMap.put("location",   b.getLocation());
                return bMap;
            }).collect(Collectors.toList());

            map.put("branches", branchList);
            return map;
        }).collect(Collectors.toList());
    }

    // ── All branches ──
    @Cacheable(value = "allBranches", key = "'all'")
    public List<Map<String, Object>> getAllBranches() {
        return branchRepo.findAll().stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",         b.getId());
            map.put("branchName", b.getBranchName());
            map.put("location",   b.getLocation());
            return map;
        }).collect(Collectors.toList());
    }

    // ── Branch password ──
    @Cacheable(value = "branchPassword", key = "#id")
    public Map<String, Object> getBranchPassword(Long id) {
        BranchUser user = branchUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        Branch branch = user.getBranches().isEmpty() ? null : user.getBranches().get(0);
        String plainPassword = branch != null ? branch.getPassword() : "Check DB";
        return Map.of("password", plainPassword);
    }

    // ── Bust branch-related caches on any mutation ──
    @CacheEvict(value = { "branchUsers", "allBranches" }, allEntries = true)
    public void evictBranchCaches() {
        // intentionally empty — AOP handles the eviction
    }
}
