package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.DuplicateResourceException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    @Mock AccountPersistencePort accountPersistencePort;
    @Mock TransactionPersistencePort transactionPersistencePort;
    @Mock UserContextPort userContextPort;
    @Mock PersistenceMapper mapper;

    @InjectMocks
    AccountService service;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
        when(userContextPort.getCurrentUser()).thenReturn(currentUser);
    }

    @Nested
    @DisplayName("getAllActiveAccounts()")
    class GetAllActiveAccounts {

        @Test
        void returnsOnlyActiveAccounts() {
            AccountItem active = AccountItem.builder().id(1L).name("Activa").active(true).build();
            AccountItem inactive = AccountItem.builder().id(2L).name("Cerrada").active(false).build();
            AccountItem nullActive = AccountItem.builder().id(3L).name("Sin flag").active(null).build();

            when(accountPersistencePort.findAllByUser(currentUser))
                    .thenReturn(List.of(active, inactive, nullActive));

            List<AccountItem> result = service.getAllActiveAccounts();

            assertThat(result).containsExactlyInAnyOrder(active, nullActive);
            assertThat(result).doesNotContain(inactive);
        }

        @Test
        void returnsEmptyListWhenNoAccounts() {
            when(accountPersistencePort.findAllByUser(currentUser)).thenReturn(List.of());
            assertThat(service.getAllActiveAccounts()).isEmpty();
        }

        @Test
        void usesCurrentUser() {
            when(accountPersistencePort.findAllByUser(currentUser)).thenReturn(List.of());
            service.getAllActiveAccounts();
            verify(userContextPort).getCurrentUser();
            verify(accountPersistencePort).findAllByUser(currentUser);
        }
    }

    @Nested
    @DisplayName("createAccount()")
    class CreateAccount {

        @Test
        void setsUserAndActiveFlagThenSaves() {
            AccountItem input = AccountItem.builder().name("Nueva cuenta").build();
            AccountItem saved = AccountItem.builder().id(10L).name("Nueva cuenta").active(true).user(currentUser).build();
            when(accountPersistencePort.save(any())).thenReturn(saved);

            AccountItem result = service.createAccount(input);

            ArgumentCaptor<AccountItem> captor = ArgumentCaptor.forClass(AccountItem.class);
            verify(accountPersistencePort).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(currentUser);
            assertThat(captor.getValue().getActive()).isTrue();
            assertThat(result.getId()).isEqualTo(10L);
        }

        @Test
        void returnsPersistedEntity() {
            AccountItem saved = AccountItem.builder().id(99L).name("Test").build();
            when(accountPersistencePort.save(any())).thenReturn(saved);
            AccountItem result = service.createAccount(AccountItem.builder().name("Test").build());
            assertThat(result.getId()).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("updateAccount()")
    class UpdateAccount {

        @Test
        void updatesFieldsAndSaves() {
            AccountItem existing = AccountItem.builder().id(1L).name("Old").user(currentUser).build();
            AccountItem details = AccountItem.builder().name("New Name").description("desc").accountType("BANK").build();
            when(accountPersistencePort.findById(1L)).thenReturn(Optional.of(existing));
            when(accountPersistencePort.findAllByUser(currentUser)).thenReturn(List.of(existing));
            when(accountPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountItem result = service.updateAccount(1L, details);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("desc");
        }

        @Test
        void throwsWhenAccountNotFound() {
            when(accountPersistencePort.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateAccount(99L, AccountItem.builder().name("x").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsOnDuplicateName() {
            AccountItem existing = AccountItem.builder().id(1L).name("Original").user(currentUser).build();
            AccountItem other = AccountItem.builder().id(2L).name("Duplicado").user(currentUser).build();
            when(accountPersistencePort.findById(1L)).thenReturn(Optional.of(existing));
            when(accountPersistencePort.findAllByUser(currentUser)).thenReturn(List.of(existing, other));

            AccountItem details = AccountItem.builder().name("Duplicado").build();
            assertThatThrownBy(() -> service.updateAccount(1L, details))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        void sameNameOnSameAccount_isAllowed() {
            AccountItem existing = AccountItem.builder().id(1L).name("MiCuenta").user(currentUser).build();
            when(accountPersistencePort.findById(1L)).thenReturn(Optional.of(existing));
            when(accountPersistencePort.findAllByUser(currentUser)).thenReturn(List.of(existing));
            when(accountPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.updateAccount(1L, AccountItem.builder().name("MiCuenta").build()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("closeAccount()")
    class CloseAccount {

        @Test
        void setsActiveFalseAndCreatesCloseTx() {
            Amount balance = new Amount(5000L, "EUR", false);
            AccountItem account = AccountItem.builder()
                    .id(1L).name("Cuenta").active(true).amount(balance).user(currentUser).build();
            when(accountPersistencePort.findById(1L)).thenReturn(Optional.of(account));

            service.closeAccount(1L);

            assertThat(account.getActive()).isFalse();
            verify(accountPersistencePort).save(account);
            ArgumentCaptor<TransactionItem> txCaptor = ArgumentCaptor.forClass(TransactionItem.class);
            verify(transactionPersistencePort).save(txCaptor.capture());
            assertThat(txCaptor.getValue().getType()).isEqualTo(TransactionItem.TransactionType.ACCOUNT_CLOSE);
            assertThat(txCaptor.getValue().getAmount().getValue()).isEqualTo(5000L);
        }

        @Test
        void doesNothingIfAlreadyInactive() {
            AccountItem account = AccountItem.builder().id(1L).active(false).build();
            when(accountPersistencePort.findById(1L)).thenReturn(Optional.of(account));

            service.closeAccount(1L);

            verify(accountPersistencePort, never()).save(any());
            verify(transactionPersistencePort, never()).save(any());
        }

        @Test
        void throwsWhenAccountNotFound() {
            when(accountPersistencePort.findById(5L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.closeAccount(5L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("permanentlyDeleteAccount()")
    class PermanentDelete {

        @Test
        void deletesAccountAndRelatedTransactions() {
            AccountItem account = AccountItem.builder().id(1L).user(currentUser).build();
            TransactionItem relatedTx = TransactionItem.builder()
                    .id(10L).account(account).user(currentUser).build();
            TransactionItem unrelated = TransactionItem.builder()
                    .id(11L).account(AccountItem.builder().id(99L).build()).user(currentUser).build();

            when(accountPersistencePort.findById(1L)).thenReturn(Optional.of(account));
            when(transactionPersistencePort.findAllByUser(eq(currentUser), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(relatedTx, unrelated)));

            service.permanentlyDeleteAccount(1L);

            verify(transactionPersistencePort).delete(relatedTx);
            verify(transactionPersistencePort, never()).delete(unrelated);
            verify(accountPersistencePort).delete(1L);
        }

        @Test
        void throwsWhenAccountNotFound() {
            when(accountPersistencePort.findById(42L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.permanentlyDeleteAccount(42L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
