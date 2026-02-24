package com.expensetracker.controller;

import com.expensetracker.model.AuthResponse;
import com.expensetracker.model.LoginRequest;
import com.expensetracker.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authenticationService.login(
                    loginRequest.getUsername(), 
                    loginRequest.getPassword()
            );
            
            AuthResponse response = new AuthResponse(token, jwtExpiration);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest registerRequest) {
        try {
            authenticationService.register(
                    registerRequest.getUsername(), 
                    registerRequest.getPassword()
            );
            return ResponseEntity.ok("User registered successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
