package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import org.springframework.stereotype.Component;

@Component
class AccountCloseImpact implements TransactionImpact {

    private final AccountPersistencePort accountPort;

    AccountCloseImpact(AccountPersistencePort accountPort) {
        this.accountPort = accountPort;
    }

    @Override
    public TransactionItem.TransactionType supportedType() {
        return TransactionItem.TransactionType.ACCOUNT_CLOSE;
    }

    @Override
    public void apply(TransactionItem transaction, User user) {
        // No balance change — account deactivation is handled in AccountService.closeAccount.
    }

    @Override
    public void revert(TransactionItem transaction, User user) {
        AccountItem account = transaction.getAccount();
        if (account == null) return;
        account.setActive(true);
        accountPort.save(account);
    }
}
