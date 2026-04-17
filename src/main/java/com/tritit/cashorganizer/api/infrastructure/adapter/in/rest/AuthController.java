package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            System.out.println("[AuthController] Login attempt: " + request.getEmail());
            return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword(), request.isRememberMe()));
        } catch (Exception e) {
            System.err.println("[AuthController] Login error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            System.out.println("[AuthController] Registering user: " + request.getEmail());
            authService.register(request.getEmail(), request.getPassword());
            return ResponseEntity.ok("Usuario registrado correctamente");
        } catch (Exception e) {
            System.err.println("[AuthController] Register Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody AuthRequest request) {
        System.out.println("[AuthController] Test endpoint - received: " + request.getEmail());
        return ResponseEntity.ok("Test OK - received: " + request.getEmail());
    }

    @Data
    public static class AuthRequest {
        private String email;
        private String password;
        private boolean rememberMe;
    }
}
