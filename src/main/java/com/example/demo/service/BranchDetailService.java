package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.BranchUser;
import com.example.demo.repository.BranchUserRepository;

@Service
@Primary
public class BranchDetailService implements UserDetailsService {

    @Autowired
    private BranchUserRepository repo;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        BranchUser user = repo.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.getBranches().size(); // ✅ force load while session is open
        return user;
    }
}