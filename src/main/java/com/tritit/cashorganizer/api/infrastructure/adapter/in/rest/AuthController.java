package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody AuthRequest request) {
        log.info("Login attempt: {}", request.getEmail());
        return authService.login(request.getEmail(), request.getPassword(), request.isRememberMe());
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        log.info("Registering user: {}", request.getEmail());
        authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Usuario registrado correctamente");
    }

    @PostMapping("/test")
    public String test(@RequestBody AuthRequest request) {
        log.info("Test endpoint received: {}", request.getEmail());
        return authService.loginTest();
    }

    @Data
    public static class AuthRequest {
        private String email;
        private String password;
        private boolean rememberMe;
    }
}
