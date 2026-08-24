package com.example.demo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JWTGenerator {

    // Use a sufficiently strong secret key.
    // In production, load this from an environment variable or secret manager.
    private static final SecretKey SECRET_KEY =
            Keys.hmacShaKeyFor(
                    "my-super-secret-key-that-is-at-least-32-characters-long"
                            .getBytes()
            );

    public static String generateToken(String username) {

        long expirationTime = 1000 * 60 * 60; // 1 hour

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .claim("role", "USER")
                .signWith(SECRET_KEY)
                .compact();
    }

    public static void main(String[] args) {

        String token = generateToken("john");

        System.out.println("JWT Token:");
        System.out.println(token);
    }
}
