package com.example.demo.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.javacpp.BytePointer;

@RestController
public class CameraController 
{

    private final VideoCapture camera;

    public CameraController() 
    {
        camera = new VideoCapture(0); 
        if (!camera.isOpened()) {
            throw new RuntimeException("Camera not found!");
        }
    }

    @GetMapping(value = "/camera", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] getFrame() 
    {
        Mat frame = new Mat();
        camera.read(frame);

        if (frame.empty()) 
        {
            throw new RuntimeException("Failed to capture frame");
        }

        BytePointer buf = new BytePointer();
        opencv_imgcodecs.imencode(".jpg", frame, buf);

        byte[] imageBytes = new byte[(int) buf.limit()];
        buf.get(imageBytes);
        buf.deallocate();

        return imageBytes;
    }
}