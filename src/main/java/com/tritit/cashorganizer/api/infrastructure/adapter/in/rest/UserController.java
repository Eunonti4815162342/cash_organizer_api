package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount() {
        authService.deleteCurrentAccount();
        return ResponseEntity.noContent().build();
    }
}
