package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.*;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Veiga <alvaroveigavazquez@gmail.com>
 */
class ReportDataServiceTest {

    // CONSTANTES PARA LAS PRUEBAS
    private static final String START_DATE = "2023-01-01";
    private static final String END_DATE = "2023-12-31";
    private static final User USER = User.builder().id(UUID.randomUUID()).email("test@test.com").build();
    private static final AccountItem ACCOUNT_1 = AccountItem.builder().id(1L).name("Account 1").build();
    private static final AccountItem ACCOUNT_2 = AccountItem.builder().id(2L).name("Account 2").build();
    private static final FinancialEntity ENTITY_1 = FinancialEntity.builder().id(1L).name("Entity 1").build();
    private static final Category CATEGORY_1 = Category.builder().id(1L).name("Category 1").financialEntity(ENTITY_1).build();
    private static final Category CATEGORY_NO_ENTITY = Category.builder().id(2L).name("Category 2").build();
    private static final Subcategory SUBCATEGORY_1 = Subcategory.builder().id(1L).name("Subcategory 1").build();
    private static final Beneficiary BENEFICIARY_1 = Beneficiary.builder().id(1L).name("Beneficiary 1").build();

    // DEFINICIÓN DE MOCKS
    private TransactionPersistencePort transactionPersistencePortMock;
    private AccountPersistencePort accountPersistencePortMock;
    private UserContextPort userContextPortMock;

    // SUJETO DE PRUEBA
    private ReportDataService sut;

    @BeforeEach
    void setUp() {
        transactionPersistencePortMock = Mockito.mock(TransactionPersistencePort.class);
        accountPersistencePortMock = Mockito.mock(AccountPersistencePort.class);
        userContextPortMock = Mockito.mock(UserContextPort.class);
        sut = new ReportDataService(transactionPersistencePortMock, accountPersistencePortMock, userContextPortMock);
    }

    @Test
    @DisplayName("getCategoryGroupedData: should return data grouped by category")
    void getCategoryGroupedData() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_1).build();
        TransactionItem t2 = TransactionItem.builder().amount(new Amount(50L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_1).build();
        TransactionItem t3 = TransactionItem.builder().amount(new Amount(30L, "EUR", false)).category(CATEGORY_NO_ENTITY).account(ACCOUNT_1).build();
        TransactionItem tNull = TransactionItem.builder().amount(new Amount(10L, "EUR", false)).category(null).account(ACCOUNT_1).build();

        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUserAndDateRange(eq(USER), eq(START_DATE), eq(END_DATE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2, t3, tNull)));

        // When
        Map<String, Long> result = sut.getCategoryGroupedData(START_DATE, END_DATE, List.of(1L), false);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("Category 1")).isEqualTo(150L);
        assertThat(result.get("Category 2")).isEqualTo(30L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUserAndDateRange(eq(USER), eq(START_DATE), eq(END_DATE), any(Pageable.class));
    }

    @Test
    @DisplayName("getCategoryGroupedData: should return data grouped by category and subcategory")
    void getCategoryGroupedDataWithSubcategory() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).category(CATEGORY_1).subcategory(SUBCATEGORY_1).account(ACCOUNT_1).build();
        
        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUser(eq(USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1)));

        // When
        Map<String, Long> result = sut.getCategoryGroupedData(null, null, null, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Category 1 > Subcategory 1")).isEqualTo(100L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUser(eq(USER), any(Pageable.class));
    }

    @Test
    @DisplayName("getEntityGroupedData: should return data grouped by financial entity")
    void getEntityGroupedData() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_1).build();
        TransactionItem t2 = TransactionItem.builder().amount(new Amount(50L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_1).build();
        TransactionItem t3 = TransactionItem.builder().amount(new Amount(30L, "EUR", false)).category(CATEGORY_NO_ENTITY).account(ACCOUNT_1).build();
        TransactionItem tOtherAccount = TransactionItem.builder().amount(new Amount(20L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_2).build();

        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUserAndDateRange(eq(USER), eq(START_DATE), eq(END_DATE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2, t3, tOtherAccount)));

        // When
        Map<String, Long> result = sut.getEntityGroupedData(START_DATE, END_DATE, List.of(1L));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Entity 1")).isEqualTo(150L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUserAndDateRange(eq(USER), eq(START_DATE), eq(END_DATE), any(Pageable.class));
    }

    @Test
    @DisplayName("getEntityGroupedData: should return all data when accountIds is null or empty")
    void getEntityGroupedDataNoAccountFilter() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_1).build();
        TransactionItem t2 = TransactionItem.builder().amount(new Amount(50L, "EUR", false)).category(CATEGORY_1).account(ACCOUNT_2).build();

        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUser(eq(USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2)));

        // When
        Map<String, Long> result = sut.getEntityGroupedData(null, null, Collections.emptyList());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Entity 1")).isEqualTo(150L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUser(eq(USER), any(Pageable.class));
    }

    @Test
    @DisplayName("getBeneficiaryGroupedData: should return data grouped by beneficiary")
    void getBeneficiaryGroupedData() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).beneficiary(BENEFICIARY_1).account(ACCOUNT_1).build();
        TransactionItem t2 = TransactionItem.builder().amount(new Amount(50L, "EUR", false)).beneficiary(BENEFICIARY_1).account(ACCOUNT_1).build();
        TransactionItem tNoBeneficiary = TransactionItem.builder().amount(new Amount(30L, "EUR", false)).beneficiary(null).account(ACCOUNT_1).build();

        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUserAndDateRange(eq(USER), eq(START_DATE), eq(END_DATE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2, tNoBeneficiary)));

        // When
        Map<String, Long> result = sut.getBeneficiaryGroupedData(START_DATE, END_DATE, List.of(1L));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Beneficiary 1")).isEqualTo(150L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUserAndDateRange(eq(USER), eq(START_DATE), eq(END_DATE), any(Pageable.class));
    }

    @Test
    @DisplayName("getBeneficiaryGroupedData: should filter by accountIds")
    void getBeneficiaryGroupedDataAccountFilter() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).beneficiary(BENEFICIARY_1).account(ACCOUNT_1).build();
        TransactionItem t2 = TransactionItem.builder().amount(new Amount(50L, "EUR", false)).beneficiary(BENEFICIARY_1).account(ACCOUNT_2).build();

        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUser(eq(USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1, t2)));

        // When
        Map<String, Long> result = sut.getBeneficiaryGroupedData(null, null, List.of(1L));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Beneficiary 1")).isEqualTo(100L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUser(eq(USER), any(Pageable.class));
    }

    @Test
    @DisplayName("getBeneficiaryGroupedData: should return all data when accountIds is null")
    void getBeneficiaryGroupedDataNullAccountIds() {
        // Given
        TransactionItem t1 = TransactionItem.builder().amount(new Amount(100L, "EUR", false)).beneficiary(BENEFICIARY_1).account(ACCOUNT_1).build();

        when(userContextPortMock.getCurrentUser()).thenReturn(USER);
        when(transactionPersistencePortMock.findAllByUser(eq(USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(t1)));

        // When
        Map<String, Long> result = sut.getBeneficiaryGroupedData(null, null, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get("Beneficiary 1")).isEqualTo(100L);
        verify(userContextPortMock).getCurrentUser();
        verify(transactionPersistencePortMock).findAllByUser(eq(USER), any(Pageable.class));
    }
}
