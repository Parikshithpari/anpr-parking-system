package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public class StreamManager 
{

    private final OCRService ocrService;
    private volatile boolean running = false;
    private Thread worker;

    public StreamManager(OCRService ocrService) 
    {
        this.ocrService = ocrService;
    }

    public void startStream() 
    {
        if (running) return;
        running = true;
        worker = new Thread(() -> 
        {
            while (running) 
            {
                try 
                {
                    String plate = ocrService.detectPlate();
                    if (plate != null) {
                        System.out.println("Detected plate: " + plate);
                    }
                    Thread.sleep(1000); 
                } 
                catch (Exception e) 
                {
                    e.printStackTrace();
                }
            }
        });
        worker.start();
    }

    public void stopStream() 
    {
        running = false;
        if (worker != null) 
        {
            worker.interrupt();
        }
    }
}