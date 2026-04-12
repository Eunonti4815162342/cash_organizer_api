package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PersistenceMapper mapper;

    private UserEntity getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public List<AccountItem> getAllActiveAccounts() {
        UserEntity user = getCurrentUserEntity();
        return accountRepository.findAllByUser(user).stream()
                .filter(a -> a.getActive() == null || a.getActive())
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountItem updateAccount(Long id, AccountItem accountDetails) {
        UserEntity user = getCurrentUserEntity();
        
        AccountEntity account = accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

        boolean nameExists = accountRepository.findAllByUser(user).stream()
                .anyMatch(a -> !a.getId().equals(id) && a.getName().equalsIgnoreCase(accountDetails.getName()));
        
        if (nameExists) {
            throw new RuntimeException("An account with this name already exists.");
        }
        
        account.setName(accountDetails.getName());
        account.setDescription(accountDetails.getDescription());
        account.setAccountType(accountDetails.getAccountType());
        account.setNotes(accountDetails.getNotes());
        account.setFlags(accountDetails.getFlags());
        if (accountDetails.getEntity() != null) {
            account.setFinancialEntity(mapper.toEntity(accountDetails.getEntity()));
        }
        
        boolean hasTransactions = transactionRepository.findAllByUser(user, Pageable.unpaged()).getContent().stream()
                .anyMatch(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) || 
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)));

        if (accountDetails.getAmount() != null) {
            account.setAmount(mapper.toEntity(accountDetails.getAmount()));
        }
        
        return mapper.toDomain(accountRepository.save(account));
    }

    @Transactional
    public void closeAccount(Long id) {
        UserEntity user = getCurrentUserEntity();
        AccountEntity account = accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

        if (account.getActive() != null && !account.getActive()) return;

        account.setActive(false);
        accountRepository.save(account);

        TransactionItem closeTx = new TransactionItem();
        closeTx.setUser(mapper.toDomain(user));
        closeTx.setAccount(mapper.toDomain(account));
        closeTx.setType(TransactionItem.TransactionType.ACCOUNT_CLOSE);
        closeTx.setDate(LocalDateTime.now().toString());
        closeTx.setDescription("Account Closed: " + account.getName());
        closeTx.setAmount(new Amount(account.getAmount().getValue(), account.getAmount().getCurrency(), false));
        closeTx.setStatusFlags(0);
        closeTx.setIsScheduled(false);
        closeTx.setIsHeader(false);
        
        transactionRepository.save(mapper.toEntity(closeTx));
    }

    @Transactional
    public void permanentlyDeleteAccount(Long id) {
        UserEntity user = getCurrentUserEntity();
        AccountEntity account = accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

        transactionRepository.findAllByUser(user, Pageable.unpaged()).getContent().stream()
                .filter(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) || 
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)))
                .forEach(transactionRepository::delete);

        accountRepository.delete(account);
    }

    @Transactional
    public AccountItem createAccount(AccountItem accountItem) {
        UserEntity user = getCurrentUserEntity();
        AccountEntity entity = mapper.toEntity(accountItem);
        entity.setUser(user);
        entity.setActive(true);
        return mapper.toDomain(accountRepository.save(entity));
    }
}
