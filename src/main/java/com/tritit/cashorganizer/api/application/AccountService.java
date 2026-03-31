package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
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

    public List<AccountItem> getAllActiveAccounts() {
        return accountRepository.findAll().stream()
                .filter(AccountItem::getActive)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountItem updateAccount(Long id, AccountItem accountDetails) {
        AccountItem account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setName(accountDetails.getName());
        account.setDescription(accountDetails.getDescription());
        account.setAccountType(accountDetails.getAccountType());
        account.setNotes(accountDetails.getNotes());
        account.setFlags(accountDetails.getFlags());
        
        // Actualizamos la moneda por si ha cambiado, pero mantenemos el valor calculado por transacciones
        if (accountDetails.getAmount() != null) {
            account.getAmount().setCurrency(accountDetails.getAmount().getCurrency());
        }
        
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccountLogically(Long id) {
        AccountItem account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getActive()) return;

        // 1. Marcar como inactiva
        account.setActive(false);
        accountRepository.save(account);

        // 2. Registrar transacción de cierre
        TransactionItem closeTx = new TransactionItem();
        closeTx.setAccount(account);
        closeTx.setType(TransactionItem.TransactionType.ACCOUNT_CLOSE);
        closeTx.setDate(LocalDateTime.now().toString());
        closeTx.setDescription("Account Closed: " + account.getName());
        closeTx.setAmount(new Amount(account.getAmount().getValue(), account.getAmount().getCurrency(), false));
        closeTx.setStatusFlags(0);
        closeTx.setIsScheduled(false);
        closeTx.setIsHeader(false);
        
        transactionRepository.save(closeTx);
    }
}
