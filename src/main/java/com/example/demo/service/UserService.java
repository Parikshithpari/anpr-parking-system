package com.example.demo.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Branch;
import com.example.demo.entity.User;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.UserRepository;


@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private BranchRepository branchRepo;

    public User registerUser(User user) 
    {
        user.setBranch(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setBalance(100.0);

        return repo.save(user);
    }


    public User userLogin(String name, String rawPassword) throws UsernameNotFoundException 
    {
        User user = repo.findUserByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) 
        {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        return user;
    }
}