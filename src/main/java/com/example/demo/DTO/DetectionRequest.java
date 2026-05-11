package com.example.demo.DTO;

public class DetectionRequest 
{
	private String plate;
	private String cameraId;
	private double confidence;
	private String timeStamp;
	
	public DetectionRequest(String plate, String cameraId, double confidence, String timeStamp) 
	{
		super();
		this.plate = plate;
		this.cameraId = cameraId;
		this.confidence = confidence;
		this.timeStamp = timeStamp;
	}

	public DetectionRequest() 
	{
		super();
	}

	public String getPlate() {
		return plate;
	}

	public void setPlate(String plate) {
		this.plate = plate;
	}

	public String getCameraId() {
		return cameraId;
	}

	public void setCameraId(String cameraId) {
		this.cameraId = cameraId;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
}