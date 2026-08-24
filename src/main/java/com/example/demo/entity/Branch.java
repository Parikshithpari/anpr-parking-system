package com.example.demo.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Branch 
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String branchName;
	
	@Column(nullable = false, unique = true)
	private String location;
	
	@Column(nullable = false, unique = true)
	private String userName;
	
	@Column(nullable = false)
	private String password;
	
	private Double pricePerMinute = 2.0;
	
	private Double specialPrice;
	
	private LocalDate specialPriceUntil;
	
	@Column(nullable = true)
	private String rtspUrl;
	
	@Column(nullable = true)
	private Boolean cameraActive = false;
	
	public Branch(Long id, String branchName, String location, String userName, String password, Double pricePerMinute, Double specialPrice, LocalDate specialPriceUntil, String rtspUrl, Boolean cameraActive) 
	{
		super();
		this.id = id;
		this.branchName = branchName;
		this.location = location;
		this.userName = userName;
		this.password = password;
		this.pricePerMinute = pricePerMinute;
		this.specialPrice = specialPrice;
		this.specialPriceUntil = specialPriceUntil;
		this.rtspUrl = rtspUrl;
		this.cameraActive = cameraActive;
	}
	
	public Double getEffectivePrice() {
	    if (specialPrice != null 
	            && specialPriceUntil != null 
	            && !LocalDate.now().isAfter(specialPriceUntil)) {
	        return specialPrice;
	    }
	    return pricePerMinute != null ? pricePerMinute : 2.0;
	}

	public Branch() 
	{
		super();
	}

	public Long getId() 
	{
		return id;
	}

	public void setId(Long id) 
	{
		this.id = id;
	}

	public String getBranchName() 
	{
		return branchName;
	}

	public void setBranchName(String branchName) 
	{
		this.branchName = branchName;
	}

	public String getLocation() 
	{
		return location;
	}

	public void setLocation(String location) 
	{
		this.location = location;
	}

	public String getUserName() 
	{
		return userName;
	}

	public void setUserName(String userName) 
	{
		this.userName = userName;
	}

	public String getPassword() 
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public Double getPricePerMinute() 
	{
		return pricePerMinute;
	}

	public void setPricePerMinute(Double pricePerMinute) 
	{
		this.pricePerMinute = pricePerMinute;
	}

	public Double getSpecialPrice() {
		return specialPrice;
	}

	public void setSpecialPrice(Double specialPrice) {
		this.specialPrice = specialPrice;
	}

	public LocalDate getSpecialPriceUntil() {
		return specialPriceUntil;
	}

	public void setSpecialPriceUntil(LocalDate specialPriceUntil) {
		this.specialPriceUntil = specialPriceUntil;
	}
	
	public String getRtspUrl() { return rtspUrl; }
	public void setRtspUrl(String rtspUrl) { this.rtspUrl = rtspUrl; }
	public Boolean getCameraActive() { return cameraActive; }
	public void setCameraActive(Boolean cameraActive) { this.cameraActive = cameraActive; }
}