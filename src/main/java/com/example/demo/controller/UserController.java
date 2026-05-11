package com.example.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;

@RestController
public class UserController 
{
	@Autowired
	private UserService service;
	
	@PostMapping("/register")
	private User registerUser(@RequestBody User user)
	{
		return service.registerUser(user);
	}
	
	@PostMapping("/userLogin")
    public User userLogin(@RequestBody Map<String, String> loginData) 
	{
        String name = loginData.get("name");
        String password = loginData.get("password");
        return service.userLogin(name, password);
    }

}
