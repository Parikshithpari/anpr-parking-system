package com.example.demo.controller;

import com.example.demo.service.OCRService;
import com.example.demo.service.StreamManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ocr")
public class OCRController 
{

    private final OCRService ocrService;
    private final StreamManager streamManager;

    public OCRController(OCRService ocrService, StreamManager streamManager) 
    {
        this.ocrService = ocrService;
        this.streamManager = streamManager;
    }

    @GetMapping("/capture")
    public ResponseEntity<?> capturePlate()
    {
        try 
        {
            String plateNumber = ocrService.detectPlate();
            return ResponseEntity.ok("Detected Plate: " + plateNumber);
        } 
        catch (Exception e) 
        {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startStream() 
    {
        streamManager.startStream();
        return ResponseEntity.ok("Stream started");
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopStream() 
    {
        streamManager.stopStream();
        return ResponseEntity.ok("Stream stopped");
    }
   
}