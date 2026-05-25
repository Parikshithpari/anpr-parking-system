package com.example.demo.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.entity.Branch;
import com.example.demo.entity.User;
import com.example.demo.repository.BranchRepository;
import com.example.demo.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;


@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BranchRepository branchRepo;

    @Autowired
    private JavaMailSender mailSender;

    // ✅ Read from application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    private final Map<String, String[]> otpStore    = new ConcurrentHashMap<>();
    private final Map<String, Integer>  otpAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long>     otpCooldown = new ConcurrentHashMap<>();

    public User registerUser(User user) {
        user.setBranch(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setBalance(0.0); // balance set by PhonePe payment
        return repo.save(user);
    }

    public User userLogin(String name, String rawPassword) {
        User user = repo.findUserByName(name)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return user;
    }

    public void sendOtp(String email) {
        // ✅ Rate limit check
        Long cooldown = otpCooldown.get(email);
        if (cooldown != null && System.currentTimeMillis() < cooldown) {
            long secondsLeft = (cooldown - System.currentTimeMillis()) / 1000;
            throw new RuntimeException(
                "Please wait " + secondsLeft + " seconds before requesting another OTP"
            );
        }

        int attempts = otpAttempts.getOrDefault(email, 0);
        if (attempts >= 3) {
            otpCooldown.put(email, System.currentTimeMillis() + (15 * 60 * 1000));
            otpAttempts.remove(email);
            throw new RuntimeException("Too many attempts. Please try again after 15 minutes.");
        }

        // ✅ Check user exists
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        String otp    = String.valueOf((int)(Math.random() * 900000) + 100000);
        long   expiry = System.currentTimeMillis() + (10 * 60 * 1000);
        otpStore.put(email, new String[]{otp, String.valueOf(expiry)});

        // 60 second cooldown between requests
        otpCooldown.put(email, System.currentTimeMillis() + (60 * 1000));
        otpAttempts.merge(email, 1, Integer::sum);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // ✅ Use injected fromEmail — not hardcoded
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("PTag — Password Reset OTP");
            helper.setText(
                "<div style='font-family:Segoe UI,sans-serif;max-width:480px;" +
                "margin:0 auto;padding:32px;background:#faf4f2;" +
                "border-radius:16px;border:1px solid rgba(139,58,58,0.2)'>" +
                "<h2 style='color:#5C1F1F;margin:0 0 8px'>PTag Password Reset</h2>" +
                "<p style='color:#9a6060;font-size:14px;margin:0 0 24px'>" +
                "Hi <strong>" + user.getName() + "</strong>, use the OTP below to reset your password.<br>" +
                "Valid for 10 minutes.</p>" +
                "<div style='background:#5C1F1F;border-radius:12px;padding:24px;" +
                "text-align:center;letter-spacing:10px;font-size:36px;" +
                "font-weight:800;color:#ffffff;font-family:monospace'>" + otp + "</div>" +
                "<p style='color:#c09090;font-size:12px;margin:20px 0 0'>" +
                "Do not share this OTP with anyone. If you did not request this, ignore this email.</p>" +
                "</div>",
                true
            );
            mailSender.send(message);
            System.out.println("✅ OTP sent to: " + email);
        } catch (Exception e) {
            System.out.println("❌ Email failed: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        String[] stored = otpStore.get(email);
        if (stored == null) return false;
        long expiry = Long.parseLong(stored[1]);
        if (System.currentTimeMillis() > expiry) {
            otpStore.remove(email);
            otpAttempts.remove(email);
            return false;
        }
        boolean valid = stored[0].equals(otp);
        if (valid) {
            otpAttempts.remove(email);
            otpCooldown.remove(email);
        }
        return valid;
    }

    public void resetPassword(String email, String otp, String newPassword) {
        if (!verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        repo.save(user);
        otpStore.remove(email);
        System.out.println("✅ Password reset for: " + email);
    }
}