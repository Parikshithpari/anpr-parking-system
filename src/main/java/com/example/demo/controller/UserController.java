package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@RestController
public class UserController 
{
	@Autowired
	private UserService service;
	
	@PostMapping("/register")
	private User registerUser(@RequestBody User user)
	{
		return service.registerUser(user);
	}
	
	// ✅ Fixed — always returns JSON
	@PostMapping("/userLogin")
	public ResponseEntity<?> userLogin(@RequestBody Map<String, String> loginData) {
	    try {
	        String name     = loginData.get("name");
	        String password = loginData.get("password");
	        User user = service.userLogin(name, password);
	        return ResponseEntity.ok(user);
	    } catch (Exception e) {
	        return ResponseEntity.status(401)
	                .body(Map.of("error", "Invalid username or password"));
	    }
	}
	
	// ✅ Send OTP
	@PostMapping("/api/user/forgot-password/send-otp")
	public ResponseEntity<Map<String, String>> sendOtp(
	        @RequestBody Map<String, String> request) {
	    try {
	        String email = request.get("email");
	        service.sendOtp(email);

	        // ✅ Return masked email for confirmation
	        String masked = maskEmail(email);
	        return ResponseEntity.ok(Map.of(
	            "message", "OTP sent successfully",
	            "maskedEmail", masked
	        ));
	    } catch (Exception e) {
	        return ResponseEntity.status(400)
	                .body(Map.of("error", e.getMessage()));
	    }
	}

	private String maskEmail(String email) {
	    int atIndex = email.indexOf("@");
	    if (atIndex <= 2) return email;
	    String local  = email.substring(0, atIndex);
	    String domain = email.substring(atIndex);
	    String masked = local.substring(0, 2)
	            + "*".repeat(local.length() - 2)
	            + domain;
	    return masked;
	}

	// ✅ Verify OTP
	@PostMapping("/api/user/forgot-password/verify-otp")
	public ResponseEntity<Map<String, Object>> verifyOtp(
	        @RequestBody Map<String, String> request) {
	    boolean valid = service.verifyOtp(
	            request.get("email"), request.get("otp"));
	    if (valid) {
	        return ResponseEntity.ok(Map.of("valid", true));
	    } else {
	        return ResponseEntity.status(400)
	                .body(Map.of("valid", false, "error", "Invalid or expired OTP"));
	    }
	}

	// ✅ Reset password
	@PostMapping("/api/user/forgot-password/reset")
	public ResponseEntity<Map<String, String>> resetPassword(
	        @RequestBody Map<String, String> request) {
	    try {
	        service.resetPassword(
	                request.get("email"),
	                request.get("otp"),
	                request.get("newPassword"));
	        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
	    } catch (Exception e) {
	        return ResponseEntity.status(400)
	                .body(Map.of("error", e.getMessage()));
	    }
	}

}
