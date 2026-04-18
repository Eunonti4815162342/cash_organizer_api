package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import org.springframework.stereotype.Component;

@Component
class ExpenseImpact extends AccountBalanceImpact {

    ExpenseImpact(AccountPersistencePort accountPort) {
        super(accountPort);
    }

    @Override
    public TransactionItem.TransactionType supportedType() {
        return TransactionItem.TransactionType.EXPENSE;
    }

    @Override
    public void apply(TransactionItem transaction, User user) {
        AccountItem from = resolveOwned(transaction.getAccount().getId(), user);
        from.withdraw(transaction.getAmount().getValue());
        accountPort.save(from);
    }

    @Override
    public void revert(TransactionItem transaction, User user) {
        AccountItem from = resolveOwned(transaction.getAccount().getId(), user);
        from.deposit(transaction.getAmount().getValue());
        accountPort.save(from);
    }
}
