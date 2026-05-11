package com.example.demo.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.javacpp.BytePointer;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
//import java.util.Optional;
//import com.example.demo.entity.User;
//import com.example.demo.entity.VehicleLog;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OCRService 
{

    private final CameraService cameraService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    public OCRService(CameraService cameraService) 
    {
        this.cameraService = cameraService;
    }
    
   /* @Autowired
    private UserRepository userRepo;

    @Autowired
    private VehicleRepository logRepo;*/

    public String detectPlate() 
    {
        Mat frame = cameraService.captureFrame();

        BytePointer buf = new BytePointer();
        opencv_imgcodecs.imencode(".jpg", frame, buf);
        byte[] imageBytes = new byte[(int) buf.limit()];
        buf.get(imageBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) 
        {
            @Override
            public String getFilename() 
            {
                return "frame.jpg";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:5000/ocr", requestEntity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) 
        {
            return (String) response.getBody().get("plateNumber");
        }
        return null;
    }
    
    /*public void processVehicleEntry(String plateNumber, Long branchId) {
        Optional<User> userOpt = userRepo.findByPlateNumberAndBranchId(plateNumber, branchId);

        if (userOpt.isPresent()) {
            VehicleLog log = new VehicleLog();
            log.setPlateNumber(plateNumber);
            log.setBranch(userOpt.get().getBranch());
            log.setEntryTime(LocalDateTime.now());
            logRepo.save(log);
        } else {
            System.out.println("Unregistered vehicle detected at branch " + branchId);
        }
    }

    public void processVehicleExit(String plateNumber, Long branchId) {
        Optional<User> userOpt = userRepo.findByPlateNumberAndBranchId(plateNumber, branchId);

        if (userOpt.isPresent()) {
            VehicleLog log = logRepo.findLatestByPlateNumberAndBranchId(plateNumber, branchId)
                    .orElseThrow(() -> new RuntimeException("No entry log found"));
            log.setExitTime(LocalDateTime.now());
            logRepo.save(log);
        } else {
            System.out.println("Unregistered vehicle exit detected at branch " + branchId);
        }
    }*/

}