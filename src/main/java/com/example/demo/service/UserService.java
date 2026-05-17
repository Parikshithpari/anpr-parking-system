package com.example.demo.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
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
public class UserService 
{

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BranchRepository branchRepo;

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String[]> otpStore = new ConcurrentHashMap<>();

    public User registerUser(User user) {
        user.setBranch(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setBalance(100.0);
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
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        long expiry = System.currentTimeMillis() + (10 * 60 * 1000);
        otpStore.put(email, new String[]{otp, String.valueOf(expiry)});

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("your-email@gmail.com");
            helper.setTo(email);
            helper.setSubject("PTag Password Reset OTP");
            helper.setText(
                "<div style='font-family:Segoe UI,sans-serif;max-width:480px;" +
                "margin:0 auto;padding:32px;background:#faf4f2;" +
                "border-radius:16px;border:1px solid rgba(139,58,58,0.2)'>" +
                "<h2 style='color:#5C1F1F;margin:0 0 8px'>PTag Password Reset</h2>" +
                "<p style='color:#9a6060;font-size:14px;margin:0 0 24px'>" +
                "Use the OTP below to reset your password. Valid for 10 minutes.</p>" +
                "<div style='background:#5C1F1F;border-radius:12px;padding:20px;" +
                "text-align:center;letter-spacing:8px;font-size:32px;" +
                "font-weight:800;color:#ffffff'>" + otp + "</div>" +
                "<p style='color:#c09090;font-size:12px;margin:20px 0 0'>" +
                "If you did not request this, ignore this email.</p></div>",
                true
            );
            mailSender.send(message);
            System.out.println("OTP sent to " + email);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    public boolean verifyOtp(String email, String otp) {
        String[] stored = otpStore.get(email);
        if (stored == null) return false;
        long expiry = Long.parseLong(stored[1]);
        if (System.currentTimeMillis() > expiry) {
            otpStore.remove(email);
            return false;
        }
        return stored[0].equals(otp);
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
        System.out.println("Password reset for: " + email);
    }
}