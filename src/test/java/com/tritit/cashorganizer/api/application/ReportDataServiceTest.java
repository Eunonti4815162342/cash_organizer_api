package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.*;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportDataService")
class ReportDataServiceTest {

    @Mock TransactionPersistencePort transactionPersistencePort;
    @Mock UserContextPort userContextPort;
    @InjectMocks ReportDataService sut;

    private static final User USER = User.builder().id(UUID.randomUUID()).email("test@test.com").build();

    private static final FinancialEntity ENTITY_BBVA = FinancialEntity.builder().id(1L).name("BBVA").build();
    private static final FinancialEntity ENTITY_ING  = FinancialEntity.builder().id(2L).name("ING").build();

    private static final AccountItem ACCOUNT_BBVA      = AccountItem.builder().id(10L).name("BBVA Corriente").entity(ENTITY_BBVA).build();
    private static final AccountItem ACCOUNT_ING       = AccountItem.builder().id(20L).name("ING Naranja").entity(ENTITY_ING).build();
    private static final AccountItem ACCOUNT_NO_ENTITY = AccountItem.builder().id(30L).name("Efectivo").build();

    private static final Beneficiary MERCADONA = Beneficiary.builder().id(1L).name("Mercadona").build();
    private static final Beneficiary AMAZON    = Beneficiary.builder().id(2L).name("Amazon").build();

    private static final Category   CAT_FOOD    = Category.builder().id(1L).name("Alimentación").build();
    private static final Category   CAT_TECH    = Category.builder().id(2L).name("Tecnología").build();
    private static final Subcategory SUBCAT_SUPER = Subcategory.builder().id(1L).name("Supermercados").category(CAT_FOOD).build();

    @BeforeEach
    void setUp() {
        when(userContextPort.getCurrentUser()).thenReturn(USER);
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private void givenTransactions(TransactionItem... transactions) {
        when(transactionPersistencePort.findAllForReport(any(), any(), any()))
                .thenReturn(List.of(transactions));
    }

    private TransactionItem expense(long value, AccountItem account) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.EXPENSE)
                .amount(new Amount(value, "EUR", true))
                .account(account)
                .date("2024-01-15")
                .build();
    }

    private TransactionItem income(long value, AccountItem account) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.INCOME)
                .amount(new Amount(value, "EUR", false))
                .account(account)
                .date("2024-01-15")
                .build();
    }

    private TransactionItem expenseWithBeneficiary(long value, AccountItem account, Beneficiary beneficiary) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.EXPENSE)
                .amount(new Amount(value, "EUR", true))
                .account(account)
                .beneficiary(beneficiary)
                .date("2024-01-15")
                .build();
    }

    private TransactionItem expenseWithCategory(long value, AccountItem account, Category category, Subcategory subcategory) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.EXPENSE)
                .amount(new Amount(value, "EUR", true))
                .account(account)
                .category(category)
                .subcategory(subcategory)
                .date("2024-01-15")
                .build();
    }

    // ─── getEntityGroupedData ──────────────────────────────────────────────

    @Nested
    @DisplayName("getEntityGroupedData()")
    class GetEntityGroupedData {

        @Test
        @DisplayName("returns empty map when there are no transactions")
        void returnsEmptyMapWhenNoTransactions() {
            givenTransactions();
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("groups expenses by entity name")
        void groupsExpensesByEntityName() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsEntry("BBVA", 500L).containsEntry("ING", 300L);
        }

        @Test
        @DisplayName("ignores income transactions — only expenses are counted")
        void ignoresIncomeTransactions() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), income(1000L, ACCOUNT_ING));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsOnlyKeys("BBVA");
        }

        @Test
        @DisplayName("falls back to 'PERSONAL / OTROS' when account has no entity")
        void fallsBackToPersonalOtrosWhenAccountHasNoEntity() {
            givenTransactions(expense(200L, ACCOUNT_NO_ENTITY));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsEntry("PERSONAL / OTROS", 200L);
        }

        @Test
        @DisplayName("falls back to 'PERSONAL / OTROS' when transaction has no account")
        void fallsBackToPersonalOtrosWhenTransactionHasNoAccount() {
            TransactionItem noAccount = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .amount(new Amount(150L, "EUR", true))
                    .date("2024-01-15")
                    .build();
            givenTransactions(noAccount);
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsEntry("PERSONAL / OTROS", 150L);
        }

        @Test
        @DisplayName("sums multiple expenses that belong to the same entity")
        void sumsMultipleExpensesForSameEntity() {
            givenTransactions(expense(200L, ACCOUNT_BBVA), expense(300L, ACCOUNT_BBVA));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsEntry("BBVA", 500L);
        }

        @Test
        @DisplayName("filters out transactions not in the provided accountIds")
        void filtersTransactionsByAccountId() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", List.of(10L));
            assertThat(result).containsOnlyKeys("BBVA");
        }

        @Test
        @DisplayName("treats null accountIds as no filter — all transactions included")
        void treatsNullAccountIdsAsNoFilter() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("treats empty accountIds as no filter — all transactions included")
        void treatsEmptyAccountIdsAsNoFilter() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", List.of());
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns only the filtered account when multiple accounts share an entity")
        void returnsOnlyFilteredAccountWhenMultipleAccountsSameEntity() {
            AccountItem secondBbva = AccountItem.builder().id(11L).name("BBVA Ahorro").entity(ENTITY_BBVA).build();
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(200L, secondBbva));
            Map<String, Long> result = sut.getEntityGroupedData("2024-01-01", "2024-01-31", List.of(10L));
            assertThat(result).containsEntry("BBVA", 500L);
        }
    }

    // ─── getBeneficiaryGroupedData ─────────────────────────────────────────

    @Nested
    @DisplayName("getBeneficiaryGroupedData()")
    class GetBeneficiaryGroupedData {

        @Test
        @DisplayName("returns empty map when there are no transactions")
        void returnsEmptyMapWhenNoTransactions() {
            givenTransactions();
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("groups expenses by beneficiary name")
        void groupsExpensesByBeneficiaryName() {
            givenTransactions(
                    expenseWithBeneficiary(500L, ACCOUNT_BBVA, MERCADONA),
                    expenseWithBeneficiary(300L, ACCOUNT_ING, AMAZON));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsEntry("Mercadona", 500L).containsEntry("Amazon", 300L);
        }

        @Test
        @DisplayName("ignores income transactions — only expenses are counted")
        void ignoresIncomeTransactions() {
            TransactionItem incomeWithBeneficiary = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.INCOME)
                    .amount(new Amount(1000L, "EUR", false))
                    .account(ACCOUNT_ING)
                    .beneficiary(AMAZON)
                    .date("2024-01-15")
                    .build();
            givenTransactions(expenseWithBeneficiary(500L, ACCOUNT_BBVA, MERCADONA), incomeWithBeneficiary);
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsOnlyKeys("Mercadona");
        }

        @Test
        @DisplayName("ignores expenses without a beneficiary")
        void ignoresTransactionsWithoutBeneficiary() {
            givenTransactions(
                    expense(500L, ACCOUNT_BBVA),
                    expenseWithBeneficiary(300L, ACCOUNT_ING, AMAZON));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsOnlyKeys("Amazon");
        }

        @Test
        @DisplayName("returns empty map when all expenses lack a beneficiary")
        void returnsEmptyWhenAllExpensesMissBeneficiary() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("sums multiple expenses with the same beneficiary")
        void sumsMultipleExpensesForSameBeneficiary() {
            givenTransactions(
                    expenseWithBeneficiary(200L, ACCOUNT_BBVA, MERCADONA),
                    expenseWithBeneficiary(300L, ACCOUNT_ING, MERCADONA));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).containsEntry("Mercadona", 500L);
        }

        @Test
        @DisplayName("filters out transactions not in the provided accountIds")
        void filtersTransactionsByAccountId() {
            givenTransactions(
                    expenseWithBeneficiary(500L, ACCOUNT_BBVA, MERCADONA),
                    expenseWithBeneficiary(300L, ACCOUNT_ING, AMAZON));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", List.of(10L));
            assertThat(result).containsOnlyKeys("Mercadona");
        }

        @Test
        @DisplayName("treats null accountIds as no filter — all transactions included")
        void treatsNullAccountIdsAsNoFilter() {
            givenTransactions(
                    expenseWithBeneficiary(500L, ACCOUNT_BBVA, MERCADONA),
                    expenseWithBeneficiary(300L, ACCOUNT_ING, AMAZON));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", null);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("treats empty accountIds as no filter — all transactions included")
        void treatsEmptyAccountIdsAsNoFilter() {
            givenTransactions(
                    expenseWithBeneficiary(500L, ACCOUNT_BBVA, MERCADONA),
                    expenseWithBeneficiary(300L, ACCOUNT_ING, AMAZON));
            Map<String, Long> result = sut.getBeneficiaryGroupedData("2024-01-01", "2024-01-31", List.of());
            assertThat(result).hasSize(2);
        }
    }

    // ─── getCategoryGroupedData ────────────────────────────────────────────

    @Nested
    @DisplayName("getCategoryGroupedData()")
    class GetCategoryGroupedData {

        @Test
        @DisplayName("groups expenses by category name")
        void groupsExpensesByCategoryName() {
            givenTransactions(
                    expenseWithCategory(500L, ACCOUNT_BBVA, CAT_FOOD, null),
                    expenseWithCategory(300L, ACCOUNT_ING, CAT_TECH, null));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, null, false);
            assertThat(result).containsEntry("Alimentación", 500L).containsEntry("Tecnología", 300L);
        }

        @Test
        @DisplayName("groups by subcategory name when groupBySubcategory is true")
        void groupsBySubcategoryWhenFlagIsTrue() {
            givenTransactions(expenseWithCategory(400L, ACCOUNT_BBVA, CAT_FOOD, SUBCAT_SUPER));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, null, true);
            assertThat(result).containsEntry("Supermercados", 400L);
        }

        @Test
        @DisplayName("falls back to category name when groupBySubcategory is true but subcategory is null")
        void fallsBackToCategoryNameWhenSubcategoryIsMissing() {
            givenTransactions(expenseWithCategory(400L, ACCOUNT_BBVA, CAT_FOOD, null));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, null, true);
            assertThat(result).containsEntry("Alimentación", 400L);
        }

        @Test
        @DisplayName("ignores income transactions — only expenses are counted")
        void ignoresIncomeTransactions() {
            givenTransactions(
                    expenseWithCategory(500L, ACCOUNT_BBVA, CAT_FOOD, null),
                    income(1000L, ACCOUNT_ING));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, null, false);
            assertThat(result).containsOnlyKeys("Alimentación");
        }

        @Test
        @DisplayName("falls back to 'Otros' when category is null")
        void fallsBackToOtrosWhenCategoryIsNull() {
            givenTransactions(expense(200L, ACCOUNT_BBVA));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, null, false);
            assertThat(result).containsEntry("Otros", 200L);
        }

        @Test
        @DisplayName("filters transactions by accountIds")
        void filtersTransactionsByAccountIds() {
            givenTransactions(
                    expenseWithCategory(500L, ACCOUNT_BBVA, CAT_FOOD, null),
                    expenseWithCategory(300L, ACCOUNT_ING, CAT_TECH, null));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", List.of(10L), null, null, false);
            assertThat(result).containsOnlyKeys("Alimentación");
        }

        @Test
        @DisplayName("filters transactions by categoryIds — matches by category")
        void filtersTransactionsByCategoryId() {
            givenTransactions(
                    expenseWithCategory(500L, ACCOUNT_BBVA, CAT_FOOD, null),
                    expenseWithCategory(300L, ACCOUNT_ING, CAT_TECH, null));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, List.of(1L), null, false);
            assertThat(result).containsOnlyKeys("Alimentación");
        }

        @Test
        @DisplayName("includes transaction when its subcategory's parent category matches the categoryId filter")
        void includesTransactionWhenSubcategoryParentMatchesCategoryFilter() {
            givenTransactions(expenseWithCategory(400L, ACCOUNT_BBVA, CAT_FOOD, SUBCAT_SUPER));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, List.of(1L), null, false);
            assertThat(result).containsEntry("Alimentación", 400L);
        }

        @Test
        @DisplayName("filters transactions by beneficiaryIds")
        void filtersTransactionsByBeneficiaryIds() {
            TransactionItem withBenef = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .amount(new Amount(500L, "EUR", true))
                    .account(ACCOUNT_BBVA)
                    .category(CAT_FOOD)
                    .beneficiary(MERCADONA)
                    .date("2024-01-15")
                    .build();
            givenTransactions(withBenef, expenseWithCategory(300L, ACCOUNT_ING, CAT_TECH, null));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, List.of(1L), false);
            assertThat(result).containsOnlyKeys("Alimentación");
        }

        @Test
        @DisplayName("sums multiple expenses in the same category")
        void sumsMultipleExpensesForSameCategory() {
            givenTransactions(
                    expenseWithCategory(200L, ACCOUNT_BBVA, CAT_FOOD, null),
                    expenseWithCategory(300L, ACCOUNT_ING, CAT_FOOD, null));
            Map<String, Long> result = sut.getCategoryGroupedData("2024-01-01", "2024-01-31", null, null, null, false);
            assertThat(result).containsEntry("Alimentación", 500L);
        }
    }

    // ─── getSegregatedReport ───────────────────────────────────────────────

    @Nested
    @DisplayName("getSegregatedReport()")
    class GetSegregatedReport {

        @Test
        @DisplayName("returns correct period string from the date parameters")
        void returnsCorrectPeriodString() {
            givenTransactions();
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getPeriod()).isEqualTo("2024-01-01 - 2024-01-31");
        }

        @Test
        @DisplayName("uses '?' for start and end when dates are null")
        void usesQuestionMarkWhenDatesAreNull() {
            givenTransactions();
            DetailedReport report = sut.getSegregatedReport(null, null, null, null, null);
            assertThat(report.getPeriod()).isEqualTo("? - ?");
        }

        @Test
        @DisplayName("strips ISO time component from the period string")
        void stripsIsoTimeFromPeriod() {
            givenTransactions();
            DetailedReport report = sut.getSegregatedReport("2024-01-01T00:00:00.000", "2024-01-31T23:59:59.999", null, null, null);
            assertThat(report.getPeriod()).isEqualTo("2024-01-01 - 2024-01-31");
        }

        @Test
        @DisplayName("calculates total expenses correctly")
        void calculatesTotalExpensesCorrectly() {
            givenTransactions(expense(300L, ACCOUNT_BBVA), expense(200L, ACCOUNT_ING));
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getTotalExpenses()).isEqualTo(500L);
        }

        @Test
        @DisplayName("calculates total incomes correctly")
        void calculatesTotalIncomesCorrectly() {
            givenTransactions(income(1000L, ACCOUNT_BBVA), income(500L, ACCOUNT_ING));
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getTotalIncomes()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("counts total transactions including both expenses and incomes")
        void countsTotalTransactions() {
            givenTransactions(expense(300L, ACCOUNT_BBVA), income(1000L, ACCOUNT_ING));
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getTotalTransactions()).isEqualTo(2);
        }

        @Test
        @DisplayName("segregates transactions by entity name and then by account name")
        void segregatesTransactionsByEntityAndAccount() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getSegregatedData()).containsKeys("BBVA", "ING");
            assertThat(report.getSegregatedData().get("BBVA")).containsKey("BBVA Corriente");
            assertThat(report.getSegregatedData().get("ING")).containsKey("ING Naranja");
        }

        @Test
        @DisplayName("groups transactions without entity under 'PERSONAL / OTROS'")
        void groupsTransactionsWithoutEntityUnderPersonalOtros() {
            givenTransactions(expense(200L, ACCOUNT_NO_ENTITY));
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getSegregatedData()).containsKey("PERSONAL / OTROS");
        }

        @Test
        @DisplayName("filters transactions by accountIds — only matching account included")
        void filtersTransactionsByAccountIds() {
            givenTransactions(expense(500L, ACCOUNT_BBVA), expense(300L, ACCOUNT_ING));
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", List.of(10L), null, null);
            assertThat(report.getSegregatedData()).containsOnlyKeys("BBVA");
            assertThat(report.getTotalExpenses()).isEqualTo(500L);
        }

        @Test
        @DisplayName("returns zero totals when there are no transactions")
        void returnsZeroTotalsWhenNoTransactions() {
            givenTransactions();
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getTotalExpenses()).isZero();
            assertThat(report.getTotalIncomes()).isZero();
            assertThat(report.getTotalTransactions()).isZero();
        }

        @Test
        @DisplayName("returns empty segregatedData when there are no transactions")
        void returnsEmptySegregatedDataWhenNoTransactions() {
            givenTransactions();
            DetailedReport report = sut.getSegregatedReport("2024-01-01", "2024-01-31", null, null, null);
            assertThat(report.getSegregatedData()).isEmpty();
        }
    }
}
