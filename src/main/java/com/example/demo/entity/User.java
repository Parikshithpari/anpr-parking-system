package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class User
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable=false)
	private String name;
	
	@Column(nullable=false, unique = true)
	private String phoneNumber;
	
	@Column(nullable=false)
	private String email;
	
	@Column(nullable=false)
	private String password;
	
	@Column(nullable=false)
	private Double balance = 0.0;
	
	@ManyToOne
	@JoinColumn(name = "branch_id", nullable = true)
	private Branch branch;
	
	@Column(nullable=false, unique = true)
	private String plateNumber;
	
	@Column(nullable=false, unique = true)
	private String RCNumber;

	@Column(nullable = true)
	private String insuranceStatus = "PENDING";
	// Values: "PENDING" | "VERIFIED" | "NOT_INSURED" | "EXPIRED"

	@Column(nullable = true)
	private String insuranceCompany;

	@Column(nullable = true)
	private String insuranceExpiry;

	@Column(nullable = true)
	private String insuranceNote;
	
	@Column(nullable = true)
	private String dateOfBirth; 

	public User() 
	{
		super();
	}

	public User(Long id, String name, String phoneNumber, String email, String password, Double balance, Branch branch,
			String plateNumber, String rCNumber, String insuranceStatus, String insuranceCompany,
			String insuranceExpiry, String insuranceNote, String dateOfBirth) 
	{
		super();
		this.id = id;
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.password = password;
		this.balance = balance;
		this.branch = branch;
		this.plateNumber = plateNumber;
		RCNumber = rCNumber;
		this.insuranceStatus = insuranceStatus;
		this.insuranceCompany = insuranceCompany;
		this.insuranceExpiry = insuranceExpiry;
		this.insuranceNote = insuranceNote;
		this.dateOfBirth = dateOfBirth;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Double getBalance() {
		return balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Branch getBranch() {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

	public String getPlateNumber() {
		return plateNumber;
	}

	public void setPlateNumber(String plateNumber) {
		this.plateNumber = plateNumber;
	}

	public String getRCNumber() {
		return RCNumber;
	}

	public void setRCNumber(String rCNumber) {
		RCNumber = rCNumber;
	}

	public String getInsuranceStatus() {
		return insuranceStatus;
	}

	public void setInsuranceStatus(String insuranceStatus) {
		this.insuranceStatus = insuranceStatus;
	}

	public String getInsuranceCompany() {
		return insuranceCompany;
	}

	public void setInsuranceCompany(String insuranceCompany) {
		this.insuranceCompany = insuranceCompany;
	}

	public String getInsuranceExpiry() {
		return insuranceExpiry;
	}

	public void setInsuranceExpiry(String insuranceExpiry) {
		this.insuranceExpiry = insuranceExpiry;
	}

	public String getInsuranceNote() {
		return insuranceNote;
	}

	public void setInsuranceNote(String insuranceNote) {
		this.insuranceNote = insuranceNote;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}
}