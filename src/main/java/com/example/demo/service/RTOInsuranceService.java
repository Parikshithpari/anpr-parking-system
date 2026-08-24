package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class RTOInsuranceService {

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.host}")
    private String rapidApiHost;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> fetchInsuranceDetails(String rcNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // ✅ FIXED — correct endpoint path
            String url = "https://" + rapidApiHost + "/getVehicleInfo";

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-rapidapi-key",  rapidApiKey);
            headers.set("x-rapidapi-host", rapidApiHost);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("vehicle_no",   rcNumber.toUpperCase().replaceAll("\\s+", ""));
            body.put("consent",      "Y");
            body.put("consent_text", "I hereby give my consent for Eccentric Labs API to fetch my information");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            // ✅ Print raw response so you can see actual field names in your console
            System.out.println("🔍 RAW RTO API RESPONSE: " + response.getBody());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            JsonNode resultNode = root.has("result") ? root.get("result") : root;

            String insuranceCompany = getTextOrNull(resultNode, "insurance_company");
            String insuranceUpto    = getTextOrNull(resultNode, "insurance_upto");
            String insurancePolicy  = getTextOrNull(resultNode, "insurance_policy_number");
            String ownerName        = getTextOrNull(resultNode, "owner_name");
            String vehicleClass     = getTextOrNull(resultNode, "vehicle_class");
            String fitnessUpto      = getTextOrNull(resultNode, "fit_up_to");

            result.put("success",          true);
            result.put("insuranceCompany", insuranceCompany);
            result.put("insuranceExpiry",  insuranceUpto);
            result.put("policyNumber",     insurancePolicy);
            result.put("ownerName",        ownerName);
            result.put("vehicleClass",     vehicleClass);
            result.put("fitnessUpto",      fitnessUpto);
            result.put("rawResponse",      response.getBody());

            if (insuranceUpto != null && !insuranceUpto.isBlank()) {
                result.put("insuranceStatus", isExpired(insuranceUpto) ? "EXPIRED" : "VERIFIED");
            } else {
                result.put("insuranceStatus", "NOT_INSURED");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            System.out.println("❌ RTO API error: " + e.getMessage());
        }

        return result;
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
            ? node.get(field).asText() : null;
    }

    private boolean isExpired(String dateStr) {
        try {
            java.time.LocalDate expiry;
            if (dateStr.contains("-") && dateStr.length() == 11) {
                java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy");
                expiry = java.time.LocalDate.parse(dateStr.trim(), fmt);
            } else {
                expiry = java.time.LocalDate.parse(dateStr.trim());
            }
            return expiry.isBefore(java.time.LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }
}