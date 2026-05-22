package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Branch;
import com.example.demo.entity.BranchUser;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.BranchUserRepository;
import com.example.demo.repository.SuperAdminRepository;

@Service
public class SuperAdminService implements UserDetailsService 
{

    @Autowired
    private SuperAdminRepository repo;
    
    @Autowired
    private BranchUserRepository branchUserRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException 
    {
        return repo.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Super admin not found"));
    }
    
    public List<Map<String, Object>> getAllBranches() {
        List<BranchUser> branches = branchUserRepo.findAll();
        return branches.stream()
        		.filter(b -> b.getBranchName() != null && !b.getBranchName().isBlank())
        		.map(b -> {
        			Map<String, Object> map = new HashMap<>();
        			map.put("id", b.getId());
        			map.put("branchName", b.getBranchName());
        			map.put("location", b.getLocation() != null ? b.getLocation() : "");
        			return map;
        		})
        		.collect(Collectors.toList());
    }
}
