package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.BranchUser;
import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.VehicleRepository;
import com.example.demo.service.VehicleLogService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/anpr")
public class AnprController {

    private final SimpMessagingTemplate messagingTemplate;
    private final VehicleLogService vehicleLogService;

    public AnprController(SimpMessagingTemplate messagingTemplate,
                          VehicleLogService vehicleLogService) {
        this.messagingTemplate = messagingTemplate;
        this.vehicleLogService = vehicleLogService;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, Object>> notifyPlate(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        String plate = request.get("plate");
        if (plate == null || plate.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Plate missing"));
        }

        BranchUser user = (BranchUser) auth.getPrincipal();

        VehicleLog saved = vehicleLogService.handleDetection(plate, null, user);

        if (saved == null) {
            return ResponseEntity.ok(Map.of("status", "ignored", "plate", plate));
        }

        // ✅ Flat map — NO branch object, NO lazy load, NO circular ref
        Map<String, Object> payload = new HashMap<>();
        payload.put("id",          saved.getId());
        payload.put("plateNumber", saved.getPlateNumber());
        payload.put("entryTime",   saved.getEntryTime() != null ? saved.getEntryTime().toString() : null);
        payload.put("exitTime",    saved.getExitTime()  != null ? saved.getExitTime().toString()  : null);
        payload.put("inside",      saved.isInside());

        // ✅ Broadcast over WebSocket
        messagingTemplate.convertAndSend("/topic/plates", (Object) payload);

        // ✅ Return same flat payload as HTTP response (not the entity)
        return ResponseEntity.ok(payload);
    }
}