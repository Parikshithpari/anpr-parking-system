package com.example.demo.controller;

import com.example.demo.entity.VehicleLog;
import com.example.demo.service.VehicleLogService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final VehicleLogService service;

    public PaymentController(VehicleLogService service) 
    {
        this.service = service;
    }

    @PostMapping("/exit/{plateNumber}")
    public Map<String, Object> processExit(@PathVariable String plateNumber) throws Exception 
    {
        VehicleLog log = service.logExitByPlate(plateNumber);

        long minutes = Duration.between(log.getEntryTime(), log.getExitTime()).toMinutes();
        double amount = calculateCharges(minutes);

        String qrData = "Plate: " + plateNumber +
                        " | Duration: " + minutes + " min" +
                        " | Amount: ₹" + amount;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        String qrBase64 = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());

        // Return JSON response
        return Map.of(
            "status", "EXIT",
            "plateNumber", plateNumber,
            "entryTime", log.getEntryTime(),
            "exitTime", log.getExitTime(),
            "durationMinutes", minutes,
            "amount", amount,
            "qrCode", qrBase64
        );
    }

    private double calculateCharges(long minutes) 
    {
        return Math.ceil(minutes / 60.0) * 50;
    }
}
