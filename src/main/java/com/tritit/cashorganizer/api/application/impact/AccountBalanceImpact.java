package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;

abstract class AccountBalanceImpact implements TransactionImpact {

    protected final AccountPersistencePort accountPort;

    protected AccountBalanceImpact(AccountPersistencePort accountPort) {
        this.accountPort = accountPort;
    }

    protected AccountItem resolveOwned(Long id, User user) {
        return accountPort.findById(id)
                .filter(a -> a.belongsTo(user))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }
}
