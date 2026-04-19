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

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested for: {}", request.getEmail());
        authService.forgotPassword(request.getEmail());
        // Always return 200 to avoid user enumeration attacks
        return ResponseEntity.ok("Si el email está registrado, recibirás un código de restablecimiento");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt with token");
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Contraseña actualizada correctamente");
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

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        private String token;
        private String newPassword;
    }
}
