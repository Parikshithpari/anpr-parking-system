package com.example.demo.entity;

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
	
	private String branchName;
	private String location;
	private String userName;
	private String password;
	
	private Double pricePerMinute = 2.0;
	
	public Branch(Long id, String branchName, String location, String userName, String password, Double pricePerMinute) 
	{
		super();
		this.id = id;
		this.branchName = branchName;
		this.location = location;
		this.userName = userName;
		this.password = password;
		this.pricePerMinute = pricePerMinute;
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
}