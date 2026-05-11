package com.example.demo.controller;

import com.example.demo.security.JwtUtil;
import com.example.demo.service.BranchDetailService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;

/*@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authManager, JwtUtil jwtUtil) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> request) {
        // Authenticate user
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.get("username"),
                request.get("password")
            )
        );
        String token = jwtUtil.generateToken(request.get("username"));

        // Run Python script in background (non-blocking)
       runPythonScript(token);

        // Return token immediately so dashboard can load
        return Map.of("token", token);
    }

    private void runPythonScript(String token) {
        new Thread(() -> {
            try {
                // Full path to Python and main.py
                ProcessBuilder pb = new ProcessBuilder(
                    "D:\\python\\python.exe",   // adjust if your Python path differs
                    "D:\\anpr\\main.py"
                );

                // Set working directory
                pb.directory(new File("D:\\anpr"));

                // Inject JWT token into environment
                Map<String, String> env = pb.environment();
                env.put("JWT_TOKEN", token);

                // Redirect stderr → stdout
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Log Python output asynchronously
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("PYTHON: " + line);
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Error running Python script:");
                e.printStackTrace();
            }
        }).start(); // run in background thread
    }
    
    @PostMapping("/api/anpr/notify")
    public ResponseEntity<String> notifyPlate(@RequestBody Map<String, String> request) 
    {
        String plate = request.get("plate");
        System.out.println("📸 Plate received: " + plate);
        return ResponseEntity.ok("Plate received");
    }

}
*/

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final BranchDetailService branchDetailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(BranchDetailService branchDetailService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.branchDetailService = branchDetailService;
        this.passwordEncoder     = passwordEncoder;
        this.jwtUtil             = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            // ✅ Load user manually
            UserDetails user = branchDetailService.loadUserByUsername(username);

            // ✅ Verify password manually
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Invalid credentials"));
            }

            // ✅ Generate token
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }
}