package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tritit.cashorganizer.api.application.TransactionService;
import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TransactionController — REST layer")
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TransactionService transactionService;

    private TransactionItem sampleTransaction() {
        return TransactionItem.builder()
                .id(1L)
                .type(TransactionItem.TransactionType.EXPENSE)
                .account(AccountItem.builder().id(1L).name("Cuenta").build())
                .amount(new Amount(5000L, "EUR", false))
                .description("Supermercado")
                .date("2024-01-15T10:00:00")
                .build();
    }

    @Nested
    @DisplayName("GET /api/transactions")
    class GetTransactions {

        @Test
        void returns200WithPagedResults() throws Exception {
            Page<TransactionItem> page = new PageImpl<>(List.of(sampleTransaction()));
            when(transactionService.getAllTransactions(any())).thenReturn(page);

            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].description").value("Supermercado"));
        }

        @Test
        void withDateRange_callsDateRangeService() throws Exception {
            Page<TransactionItem> page = new PageImpl<>(List.of());
            when(transactionService.getTransactionsByDateRange(eq("2024-01-01"), eq("2024-01-31"), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/transactions?startDate=2024-01-01&endDate=2024-01-31"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactionsByDateRange(eq("2024-01-01"), eq("2024-01-31"), any());
            verify(transactionService, never()).getAllTransactions(any());
        }

        @Test
        void withDateRangeAndAccountIds_callsAccountDateRangeService() throws Exception {
            Page<TransactionItem> page = new PageImpl<>(List.of());
            when(transactionService.getTransactionsByAccountAndDateRange(any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/transactions?startDate=2024-01-01&endDate=2024-01-31&accountIds=1,2"))
                    .andExpect(status().isOk());

            verify(transactionService).getTransactionsByAccountAndDateRange(any(), eq("2024-01-01"), eq("2024-01-31"), any());
        }

        @Test
        void withoutDates_callsGetAll() throws Exception {
            when(transactionService.getAllTransactions(any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isOk());

            verify(transactionService).getAllTransactions(any());
        }

        @Test
        void paginationDefaultsApplied() throws Exception {
            when(transactionService.getAllTransactions(any())).thenReturn(new PageImpl<>(List.of()));
            mockMvc.perform(get("/api/transactions"))
                    .andExpect(status().isOk());

            verify(transactionService).getAllTransactions(argThat(p ->
                    p.getPageNumber() == 0 && p.getPageSize() == 20
            ));
        }
    }

    @Nested
    @DisplayName("POST /api/transactions")
    class CreateTransaction {

        @Test
        void returns200WithCreatedTransaction() throws Exception {
            TransactionItem input = sampleTransaction();
            input.setId(null);
            TransactionItem saved = sampleTransaction();
            when(transactionService.createTransaction(any())).thenReturn(saved);

            mockMvc.perform(post("/api/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        void returns400WhenAccountIsNull() throws Exception {
            when(transactionService.createTransaction(any()))
                    .thenThrow(new InvalidTransactionException("Source account is mandatory"));

            mockMvc.perform(post("/api/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionItem.builder().build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Source account is mandatory"));
        }

        @Test
        void returns400WhenAmountIsZero() throws Exception {
            when(transactionService.createTransaction(any()))
                    .thenThrow(new InvalidTransactionException("Amount must be non-zero"));

            mockMvc.perform(post("/api/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionItem.builder().build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Amount must be non-zero"));
        }

        @Test
        void returns400WhenTransferHasNoDestination() throws Exception {
            when(transactionService.createTransaction(any()))
                    .thenThrow(new InvalidTransactionException("Destination account is mandatory for transfers"));

            mockMvc.perform(post("/api/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionItem.builder().build())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/transactions/{id}")
    class UpdateTransaction {

        @Test
        void returns200WithUpdatedTransaction() throws Exception {
            TransactionItem updated = sampleTransaction();
            updated.setDescription("Updated");
            when(transactionService.updateTransaction(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/transactions/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updated)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Updated"));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(transactionService.updateTransaction(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Transaction not found"));

            mockMvc.perform(put("/api/transactions/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleTransaction())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/transactions/{id}")
    class DeleteTransaction {

        @Test
        void returns200OnSuccess() throws Exception {
            mockMvc.perform(delete("/api/transactions/1"))
                    .andExpect(status().isOk());
            verify(transactionService).deleteTransaction(1L);
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Transaction not found"))
                    .when(transactionService).deleteTransaction(99L);

            mockMvc.perform(delete("/api/transactions/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
