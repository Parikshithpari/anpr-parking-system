package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.demo.repository.SuperAdminRepository;

@Service
public class SuperAdminService implements UserDetailsService 
{

    @Autowired
    private SuperAdminRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException 
    {
        return repo.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Super admin not found"));
    }
}
