package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.DuplicateResourceException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.in.AccountUseCase;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.FinancialEntityPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService implements AccountUseCase {

    private final AccountPersistencePort accountPersistencePort;
    private final TransactionPersistencePort transactionPersistencePort;
    private final FinancialEntityPersistencePort financialEntityPersistencePort;
    private final UserContextPort userContextPort;
    private final PersistenceMapper mapper;

    @Override
    public List<AccountItem> getAllActiveAccounts() {
        User user = userContextPort.getCurrentUser();
        return accountPersistencePort.findAllByUser(user).stream()
                .filter(a -> a.getActive() == null || a.getActive())
                .toList();
    }

    @Override
    @Transactional
    public AccountItem updateAccount(Long id, AccountItem accountDetails) {
        User user = userContextPort.getCurrentUser();

        AccountItem account = accountPersistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        boolean nameExists = accountPersistencePort.findAllByUser(user).stream()
                .anyMatch(a -> !a.getId().equals(id) && a.getName().equalsIgnoreCase(accountDetails.getName()));

        if (nameExists) {
            throw new DuplicateResourceException("An account with this name already exists.");
        }

        // VALIDACIÓN Y ASOCIACIÓN DE ENTIDAD
        if (accountDetails.getEntity() != null && accountDetails.getEntity().getId() != null) {
            var entity = financialEntityPersistencePort.findById(accountDetails.getEntity().getId())
                    .filter(e -> e.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Financial Entity not found or unauthorized"));
            account.setEntity(entity);
        } else {
            account.setEntity(null);
        }

        account.setName(accountDetails.getName());
        account.setDescription(accountDetails.getDescription());
        account.setAccountType(accountDetails.getAccountType());
        account.setNotes(accountDetails.getNotes());
        account.setFlags(accountDetails.getFlags());
        account.setAmount(accountDetails.getAmount());

        return accountPersistencePort.save(account);
    }

    @Override
    @Transactional
    public void closeAccount(Long id) {
        User user = userContextPort.getCurrentUser();
        AccountItem account = accountPersistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getActive() != null && !account.getActive()) return;

        account.setActive(false);
        accountPersistencePort.save(account);

        TransactionItem closeTx = new TransactionItem();
        closeTx.setUser(user);
        closeTx.setAccount(account);
        closeTx.setType(TransactionItem.TransactionType.ACCOUNT_CLOSE);
        closeTx.setDate(LocalDateTime.now().toString());
        closeTx.setDescription("Account Closed: " + account.getName());
        closeTx.setAmount(new Amount(account.getAmount().getValue(), account.getAmount().getCurrency(), false));
        closeTx.setStatusFlags(0);
        closeTx.setIsScheduled(false);
        closeTx.setIsHeader(false);

        transactionPersistencePort.save(closeTx);
    }

    @Override
    @Transactional
    public void permanentlyDeleteAccount(Long id) {
        User user = userContextPort.getCurrentUser();
        AccountItem account = accountPersistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        transactionPersistencePort.findAllByUser(user, Pageable.unpaged()).getContent().stream()
                .filter(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) ||
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)))
                .forEach(transactionPersistencePort::delete);

        accountPersistencePort.delete(id);
    }

    @Override
    @Transactional
    public AccountItem createAccount(AccountItem accountItem) {
        User user = userContextPort.getCurrentUser();
        accountItem.setUser(user);
        accountItem.setActive(true);

        // VALIDACIÓN Y ASOCIACIÓN DE ENTIDAD
        if (accountItem.getEntity() != null && accountItem.getEntity().getId() != null) {
            var entity = financialEntityPersistencePort.findById(accountItem.getEntity().getId())
                    .filter(e -> e.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Financial Entity not found or unauthorized"));
            accountItem.setEntity(entity);
        }

        return accountPersistencePort.save(accountItem);
    }
}
