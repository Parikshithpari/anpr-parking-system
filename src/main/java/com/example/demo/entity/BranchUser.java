package com.example.demo.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Entity
public class BranchUser implements UserDetails
{
	 @Id
	 @GeneratedValue(strategy = GenerationType.IDENTITY)
	 private Long id;
	 
	 @Column(nullable = false, unique = true)
	 private String branchName;
	 
	 @Column(nullable = false, unique = true)
	 private String location;
	 
	 @Column(name = "user_name", nullable = false, unique= true)
	 private String userName;
	 private String password;

	 @ManyToMany(fetch = FetchType.EAGER)
	 @JoinTable(
	     name = "branch_user_branches",
	     joinColumns = @JoinColumn(name = "branch_user_id"),
	     inverseJoinColumns = @JoinColumn(name = "branch_id")
	 )
	 private List<Branch> branches = new ArrayList<>();

	
	@Override
    public Collection<? extends GrantedAuthority> getAuthorities()
	{
        return Collections.emptyList(); 
    }

	@Override
    public String getPassword() 
	{
        return password;
    }

	@Override
	public String getUsername() 
	{
		return userName;
	}

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
    

	public BranchUser(Long id, String branchName, String location, String userName, String password,
			List<Branch> branches) 
	{
		super();
		this.id = id;
		this.branchName = branchName;
		this.location = location;
		this.userName = userName;
		this.password = password;
		this.branches = branches;
	}

	public BranchUser() 
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


	public void setUserName(String userName) 
	{
		this.userName = userName;
	}

	public void setPassword(String password) 
	{
		this.password = password;
	}

	public List<Branch> getBranches()
	{
		return branches;
	}

	public void setBranches(List<Branch> branches) 
	{
		this.branches = branches;
	}

}