package com.example.demo.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class VehicleLog 
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String plateNumber;
	private LocalDateTime entryTime;
	private LocalDateTime exitTime;
	private boolean inside;
	private Double price;
	
	@ManyToOne
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	public VehicleLog(Long id, String plateNumber, LocalDateTime entryTime, LocalDateTime exitTime, boolean inside,
			Branch branch, Double price) 
	{
		super();
		this.id = id;
		this.plateNumber = plateNumber;
		this.entryTime = entryTime;
		this.exitTime = exitTime;
		this.inside = inside;
		this.branch = branch;
		this.price = price;
	}

	public VehicleLog() 
	{
		super();
	}
	
	public void calculatePrice(double pricePerMinute) 
	{
	    if (entryTime != null && exitTime != null)
	    {
	        long minutes = java.time.Duration.between(entryTime, exitTime).toMinutes();
	        if (minutes < 1) minutes = 1;
	        this.price = minutes * pricePerMinute;
	    }
	}

	public Long getId() 
	{
		return id;
	}

	public void setId(Long id) 
	{
		this.id = id;
	}

	public String getPlateNumber() 
	{
		return plateNumber;
	}

	public void setPlateNumber(String plateNumber) 
	{
		this.plateNumber = plateNumber;
	}

	public LocalDateTime getEntryTime() 
	{
		return entryTime;
	}

	public void setEntryTime(LocalDateTime entryTime) 
	{
		this.entryTime = entryTime;
	}

	public LocalDateTime getExitTime() 
	{
		return exitTime;
	}

	public void setExitTime(LocalDateTime exitTime) 
	{
		this.exitTime = exitTime;
	}

	public boolean isInside() 
	{
		return inside;
	}

	public void setInside(boolean inside) 
	{
		this.inside = inside;
	}

	public Branch getBranch() 
	{
		return branch;
	}

	public void setBranch(Branch branch) 
	{
		this.branch = branch;
	}

	public Double getPrice() 
	{
		return price;
	}

	public void setPrice(Double price) 
	{
		this.price = price;
	}
}