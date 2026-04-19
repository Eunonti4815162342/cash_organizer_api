package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tritit.cashorganizer.api.application.AuthService;
import com.tritit.cashorganizer.api.domain.exception.AuthenticationFailedException;
import com.tritit.cashorganizer.api.domain.exception.DuplicateResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController — REST layer")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        void returns200WithTokenOnSuccess() throws Exception {
            when(authService.login("user@test.com", "pass123", false))
                    .thenReturn(Map.of("token", "jwt_token", "email", "user@test.com"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("user@test.com", "pass123")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt_token"))
                    .andExpect(jsonPath("$.email").value("user@test.com"));
        }

        @Test
        void returns401WhenCredentialsInvalid() throws Exception {
            when(authService.login(anyString(), anyString(), anyBoolean()))
                    .thenThrow(new AuthenticationFailedException("Credenciales inválidas"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("user@test.com", "wrongpass")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
        }

        @Test
        void returns401WhenUserNotFound() throws Exception {
            when(authService.login(anyString(), anyString(), anyBoolean()))
                    .thenThrow(new AuthenticationFailedException("Credenciales inválidas"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("nobody@test.com", "pass")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void passesRememberMeFlag() throws Exception {
            when(authService.login(anyString(), anyString(), eq(true)))
                    .thenReturn(Map.of("token", "t", "email", "e"));

            String body = objectMapper.writeValueAsString(
                    Map.of("email", "user@test.com", "password", "pass", "rememberMe", true));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            verify(authService).login("user@test.com", "pass", true);
        }

        @Test
        void responseContainsBothTokenAndEmail() throws Exception {
            when(authService.login(any(), any(), anyBoolean()))
                    .thenReturn(Map.of("token", "abc123", "email", "me@test.com"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("me@test.com", "pass")))
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.email").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        void returns200OnSuccess() throws Exception {
            doNothing().when(authService).register(anyString(), anyString());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("new@test.com", "newpass")))
                    .andExpect(status().isOk());
        }

        @Test
        void returns409WhenEmailAlreadyExists() throws Exception {
            doThrow(new DuplicateResourceException("El usuario ya existe"))
                    .when(authService).register(anyString(), anyString());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("existing@test.com", "pass")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("El usuario ya existe"));
        }

        @Test
        void callsServiceWithEmailAndPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("new@test.com", "securePass")));

            verify(authService).register("new@test.com", "securePass");
        }
    }

    @Nested
    @DisplayName("POST /api/auth/test")
    class TestEndpoint {

        @Test
        void returns200WithString() throws Exception {
            when(authService.loginTest()).thenReturn("Test: Secret key is configured");

            mockMvc.perform(post("/api/auth/test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("any@test.com", "any")))
                    .andExpect(status().isOk());
        }
    }
}
