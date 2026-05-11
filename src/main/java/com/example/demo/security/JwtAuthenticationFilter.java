package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.BranchDetailService;
import com.example.demo.service.SuperAdminService;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter 
{

    private final JwtUtil jwtUtil;
    private final BranchDetailService branchDetailService;
    private final SuperAdminService superAdminDetailService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   BranchDetailService branchDetailService,
                                   SuperAdminService superAdminDetailService) 
    {
        this.jwtUtil                  = jwtUtil;
        this.branchDetailService      = branchDetailService;
        this.superAdminDetailService  = superAdminDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException
    {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String token    = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) 
        {
            token    = authHeader.substring(7);
            username = jwtUtil.extractUsername(token);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = null;

            // ✅ Try branch admin first
            try 
            {
                userDetails = branchDetailService.loadUserByUsername(username);
            } 
            catch (Exception e) 
            {
               
            }

            if (userDetails == null) 
            {
                try 
                {
                    userDetails = superAdminDetailService.loadUserByUsername(username);
                } 
                catch (Exception e) 
                {

                }
            }

            if (userDetails != null && jwtUtil.validateToken(token)) 
            {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        chain.doFilter(request, response);
    }
}