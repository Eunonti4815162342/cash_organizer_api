package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tritit.cashorganizer.api.application.AccountService;
import com.tritit.cashorganizer.api.domain.exception.DuplicateResourceException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AccountController — REST layer")
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AccountService accountService;

    private AccountItem sampleAccount() {
        return AccountItem.builder()
                .id(1L)
                .name("Cuenta corriente")
                .accountType("BANK")
                .amount(new Amount(10000L, "EUR", false))
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("GET /api/accounts")
    class GetAccounts {

        @Test
        void returns200WithList() throws Exception {
            when(accountService.getAllActiveAccounts()).thenReturn(List.of(sampleAccount()));

            mockMvc.perform(get("/api/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Cuenta corriente"));
        }

        @Test
        void returns200WithEmptyList() throws Exception {
            when(accountService.getAllActiveAccounts()).thenReturn(List.of());
            mockMvc.perform(get("/api/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void returnsMultipleAccounts() throws Exception {
            AccountItem second = AccountItem.builder().id(2L).name("Ahorros").build();
            when(accountService.getAllActiveAccounts()).thenReturn(List.of(sampleAccount(), second));

            mockMvc.perform(get("/api/accounts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    @Nested
    @DisplayName("POST /api/accounts")
    class CreateAccount {

        @Test
        void returns200WithCreatedAccount() throws Exception {
            AccountItem input = AccountItem.builder().name("Nueva").accountType("CASH").build();
            AccountItem saved = AccountItem.builder().id(10L).name("Nueva").accountType("CASH").build();
            when(accountService.createAccount(any())).thenReturn(saved);

            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.name").value("Nueva"));
        }

        @Test
        void callsServiceWithProvidedBody() throws Exception {
            AccountItem input = AccountItem.builder().name("Test").build();
            when(accountService.createAccount(any())).thenReturn(input);

            mockMvc.perform(post("/api/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(input)));

            verify(accountService).createAccount(any(AccountItem.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/accounts/{id}")
    class UpdateAccount {

        @Test
        void returns200WithUpdatedAccount() throws Exception {
            AccountItem updated = AccountItem.builder().id(1L).name("Actualizada").build();
            when(accountService.updateAccount(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/accounts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Actualizada"));
        }

        @Test
        void returns404WhenAccountNotFound() throws Exception {
            when(accountService.updateAccount(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Account not found"));

            mockMvc.perform(put("/api/accounts/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(AccountItem.builder().name("x").build())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Account not found"));
        }

        @Test
        void returns409OnDuplicateName() throws Exception {
            when(accountService.updateAccount(eq(1L), any()))
                    .thenThrow(new DuplicateResourceException("An account with this name already exists."));

            mockMvc.perform(put("/api/accounts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(AccountItem.builder().name("dup").build())))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/accounts/{id}")
    class DeleteAccount {

        @Test
        void softDeleteByDefault() throws Exception {
            mockMvc.perform(delete("/api/accounts/1"))
                    .andExpect(status().isOk());
            verify(accountService).closeAccount(1L);
            verify(accountService, never()).permanentlyDeleteAccount(any());
        }

        @Test
        void permanentDeleteWhenParamIsTrue() throws Exception {
            mockMvc.perform(delete("/api/accounts/1?permanent=true"))
                    .andExpect(status().isOk());
            verify(accountService).permanentlyDeleteAccount(1L);
            verify(accountService, never()).closeAccount(any());
        }

        @Test
        void returns404WhenAccountNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Account not found"))
                    .when(accountService).closeAccount(99L);

            mockMvc.perform(delete("/api/accounts/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/accounts/{id}/permanent")
    class PermanentDelete {

        @Test
        void callsPermanentDelete() throws Exception {
            mockMvc.perform(delete("/api/accounts/5/permanent"))
                    .andExpect(status().isOk());
            verify(accountService).permanentlyDeleteAccount(5L);
        }

        @Test
        void returns404WhenAccountNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Account not found"))
                    .when(accountService).permanentlyDeleteAccount(99L);

            mockMvc.perform(delete("/api/accounts/99/permanent"))
                    .andExpect(status().isNotFound());
        }
    }
}
