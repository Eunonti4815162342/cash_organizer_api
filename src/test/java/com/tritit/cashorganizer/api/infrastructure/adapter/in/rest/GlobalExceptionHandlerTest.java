package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.AccountService;
import com.tritit.cashorganizer.api.domain.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GlobalExceptionHandler — HTTP status mapping")
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AccountService accountService;

    @Test
    @DisplayName("ResourceNotFoundException → 404 NOT FOUND")
    void resourceNotFound_returns404() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new ResourceNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("AuthenticationFailedException → 401 UNAUTHORIZED")
    void authFailed_returns401() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new AuthenticationFailedException("Credenciales inválidas"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("AccessDeniedException → 403 FORBIDDEN")
    void accessDenied_returns403() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("InvalidTransactionException → 400 BAD REQUEST")
    void invalidTransaction_returns400() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new InvalidTransactionException("Amount must be non-zero"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Amount must be non-zero"));
    }

    @Test
    @DisplayName("DuplicateResourceException → 409 CONFLICT")
    void duplicate_returns409() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new DuplicateResourceException("Name already exists"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Unexpected exception → 500 INTERNAL SERVER ERROR")
    void unexpectedException_returns500() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new RuntimeException("Something went very wrong"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    @DisplayName("Error response always includes timestamp, status, error, message")
    void responseBodyHasRequiredFields() throws Exception {
        when(accountService.getAllActiveAccounts())
                .thenThrow(new ResourceNotFoundException("Not found"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
