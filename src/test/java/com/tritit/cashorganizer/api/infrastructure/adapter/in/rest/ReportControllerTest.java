package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.ReportService;
import com.tritit.cashorganizer.api.infrastructure.config.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReportController — REST layer")
class ReportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ReportService reportService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    // ─── GET /api/reports/entity-stats ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/reports/entity-stats")
    class GetEntityStats {

        @Test
        @DisplayName("returns 200 with empty map when service returns no data")
        void returns200WithEmptyMap() throws Exception {
            when(reportService.getEntityGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/entity-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isMap())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("returns 200 with populated map when service returns data")
        void returns200WithPopulatedMap() throws Exception {
            when(reportService.getEntityGroupedData(any(), any(), any()))
                    .thenReturn(Map.of("BBVA", 500L, "ING", 300L));
            mockMvc.perform(get("/api/reports/entity-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.BBVA").value(500))
                    .andExpect(jsonPath("$.ING").value(300));
        }

        @Test
        @DisplayName("passes startDate and endDate to the service")
        void passesDateRangeToService() throws Exception {
            when(reportService.getEntityGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/entity-stats?startDate=2024-01-01&endDate=2024-01-31"))
                    .andExpect(status().isOk());
            verify(reportService).getEntityGroupedData("2024-01-01", "2024-01-31", null);
        }

        @Test
        @DisplayName("sanitizes ISO datetime to date-only before passing to service")
        void sanitizesIsoDatetimeToDateOnly() throws Exception {
            when(reportService.getEntityGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/entity-stats?startDate=2024-01-01T00:00:00.000&endDate=2024-01-31T23:59:59.999"))
                    .andExpect(status().isOk());
            verify(reportService).getEntityGroupedData("2024-01-01", "2024-01-31", null);
        }

        @Test
        @DisplayName("passes accountIds list to the service")
        void passesAccountIdsToService() throws Exception {
            when(reportService.getEntityGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/entity-stats?accountIds=10&accountIds=20"))
                    .andExpect(status().isOk());
            verify(reportService).getEntityGroupedData(isNull(), isNull(), eq(List.of(10L, 20L)));
        }

        @Test
        @DisplayName("passes null for all params when called with no query parameters")
        void passesNullsWhenNoQueryParams() throws Exception {
            when(reportService.getEntityGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/entity-stats"))
                    .andExpect(status().isOk());
            verify(reportService).getEntityGroupedData(isNull(), isNull(), isNull());
        }
    }

    // ─── GET /api/reports/beneficiary-stats ───────────────────────────────

    @Nested
    @DisplayName("GET /api/reports/beneficiary-stats")
    class GetBeneficiaryStats {

        @Test
        @DisplayName("returns 200 with empty map when service returns no data")
        void returns200WithEmptyMap() throws Exception {
            when(reportService.getBeneficiaryGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/beneficiary-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isMap())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("returns 200 with populated map when service returns data")
        void returns200WithPopulatedMap() throws Exception {
            when(reportService.getBeneficiaryGroupedData(any(), any(), any()))
                    .thenReturn(Map.of("Mercadona", 500L, "Amazon", 300L));
            mockMvc.perform(get("/api/reports/beneficiary-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.Mercadona").value(500))
                    .andExpect(jsonPath("$.Amazon").value(300));
        }

        @Test
        @DisplayName("passes startDate and endDate to the service")
        void passesDateRangeToService() throws Exception {
            when(reportService.getBeneficiaryGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/beneficiary-stats?startDate=2024-01-01&endDate=2024-01-31"))
                    .andExpect(status().isOk());
            verify(reportService).getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
        }

        @Test
        @DisplayName("sanitizes ISO datetime to date-only before passing to service")
        void sanitizesIsoDatetimeToDateOnly() throws Exception {
            when(reportService.getBeneficiaryGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/beneficiary-stats?startDate=2024-01-01T00:00:00.000&endDate=2024-01-31T23:59:59.999"))
                    .andExpect(status().isOk());
            verify(reportService).getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
        }

        @Test
        @DisplayName("passes accountIds list to the service")
        void passesAccountIdsToService() throws Exception {
            when(reportService.getBeneficiaryGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/beneficiary-stats?accountIds=10&accountIds=20"))
                    .andExpect(status().isOk());
            verify(reportService).getBeneficiaryGroupedData(isNull(), isNull(), eq(List.of(10L, 20L)));
        }

        @Test
        @DisplayName("passes null for all params when called with no query parameters")
        void passesNullsWhenNoQueryParams() throws Exception {
            when(reportService.getBeneficiaryGroupedData(any(), any(), any())).thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/beneficiary-stats"))
                    .andExpect(status().isOk());
            verify(reportService).getBeneficiaryGroupedData(isNull(), isNull(), isNull());
        }
    }

    // ─── GET /api/reports/category-stats ──────────────────────────────────

    @Nested
    @DisplayName("GET /api/reports/category-stats")
    class GetCategoryStats {

        @Test
        @DisplayName("returns 200 with populated map when service returns data")
        void returns200WithPopulatedMap() throws Exception {
            when(reportService.getCategoryGroupedData(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Map.of("Alimentación", 500L));
            mockMvc.perform(get("/api/reports/category-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.Alimentación").value(500));
        }

        @Test
        @DisplayName("returns 200 with empty map when service returns no data")
        void returns200WithEmptyMap() throws Exception {
            when(reportService.getCategoryGroupedData(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/category-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("defaults groupBySubcategory to false when not provided")
        void defaultsGroupBySubcategoryToFalse() throws Exception {
            when(reportService.getCategoryGroupedData(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/category-stats"))
                    .andExpect(status().isOk());
            verify(reportService).getCategoryGroupedData(any(), any(), any(), any(), any(), eq(false));
        }

        @Test
        @DisplayName("passes groupBySubcategory=true to service when flag is set")
        void passesGroupBySubcategoryTrueToService() throws Exception {
            when(reportService.getCategoryGroupedData(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/category-stats?groupBySubcategory=true"))
                    .andExpect(status().isOk());
            verify(reportService).getCategoryGroupedData(any(), any(), any(), any(), any(), eq(true));
        }

        @Test
        @DisplayName("sanitizes ISO datetime to date-only before passing to service")
        void sanitizesIsoDatetimeToDateOnly() throws Exception {
            when(reportService.getCategoryGroupedData(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/category-stats?startDate=2024-06-01T00:00:00.000&endDate=2024-06-30T23:59:59.999"))
                    .andExpect(status().isOk());
            verify(reportService).getCategoryGroupedData(eq("2024-06-01"), eq("2024-06-30"), any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("passes accountIds, categoryIds and beneficiaryIds to the service")
        void passesAllFilterIdsToService() throws Exception {
            when(reportService.getCategoryGroupedData(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Map.of());
            mockMvc.perform(get("/api/reports/category-stats?accountIds=10&categoryIds=1&beneficiaryIds=2"))
                    .andExpect(status().isOk());
            verify(reportService).getCategoryGroupedData(
                    isNull(), isNull(),
                    eq(List.of(10L)),
                    eq(List.of(1L)),
                    eq(List.of(2L)),
                    eq(false));
        }
    }
}
