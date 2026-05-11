package com.example.demo.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CameraService 
{
    private VideoCapture camera;

    @PostConstruct
    public void init() 
    {
        initCamera(0);
    }

    public void initCamera(int deviceIndex) 
    {
        camera = new VideoCapture(deviceIndex);
        if (!camera.isOpened()) 
        {
            throw new RuntimeException("Cannot open the camera with index " + deviceIndex);
        }
    }

    public void initCamera(String streamUrl) 
    {
        camera = new VideoCapture(streamUrl);
        if (!camera.isOpened())
        {
            throw new RuntimeException("Cannot open the camera stream " + streamUrl);
        }
    }

    public Mat captureFrame() 
    {
        if (camera == null || !camera.isOpened()) 
        {
            throw new RuntimeException("Camera is not initialized");
        }
        Mat frame = new Mat();
        camera.read(frame);
        if (frame.empty()) 
        {
            throw new RuntimeException("Failed to capture frame");
        }
        return frame;
    }

    @PreDestroy
    public void cleanup() 
    {
        if (camera != null && camera.isOpened()) 
        {
            camera.release();
        }
    }
}