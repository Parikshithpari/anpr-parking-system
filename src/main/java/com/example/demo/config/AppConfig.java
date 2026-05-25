package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.phonepe.sdk.pg.Env;                                          // ✅ correct
import com.phonepe.sdk.pg.payments.v2.StandardCheckoutClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {

    @Value("${phonepe.client-id}")
    private String clientId;

    @Value("${phonepe.client-secret}")
    private String clientSecret;

    @Value("${phonepe.client-version}")
    private int clientVersion;

    @Value("${phonepe.environment}")
    private String environment;

    @Bean
    public StandardCheckoutClient phonePeClient() {
        Env env = environment.equalsIgnoreCase("PRODUCTION")
                ? Env.PRODUCTION
                : Env.SANDBOX;

        return StandardCheckoutClient.getInstance(   // ✅ getInstance, not init
                clientId,
                clientSecret,
                clientVersion,
                env
        );
    }

    @Bean
    public Map<String, Map<String, String>> pendingTopUps() {
        return new ConcurrentHashMap<>();
    }
}