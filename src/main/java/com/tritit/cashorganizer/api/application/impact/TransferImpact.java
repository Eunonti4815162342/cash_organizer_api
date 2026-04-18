package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import org.springframework.stereotype.Component;

@Component
class TransferImpact extends AccountBalanceImpact {

    TransferImpact(AccountPersistencePort accountPort) {
        super(accountPort);
    }

    @Override
    public TransactionItem.TransactionType supportedType() {
        return TransactionItem.TransactionType.TRANSFER;
    }

    @Override
    public void apply(TransactionItem transaction, User user) {
        if (transaction.getToAccount() == null) {
            throw new InvalidTransactionException("Destination account is mandatory for transfers");
        }
        AccountItem from = resolveOwned(transaction.getAccount().getId(), user);
        AccountItem to = resolveOwned(transaction.getToAccount().getId(), user);
        long val = transaction.getAmount().getValue();
        from.setAmount(from.getAmount().subtract(val));
        to.setAmount(to.getAmount().add(val));
        accountPort.save(from);
        accountPort.save(to);
    }

    @Override
    public void revert(TransactionItem transaction, User user) {
        if (transaction.getToAccount() == null) return;
        AccountItem from = resolveOwned(transaction.getAccount().getId(), user);
        AccountItem to = resolveOwned(transaction.getToAccount().getId(), user);
        long val = transaction.getAmount().getValue();
        from.setAmount(from.getAmount().add(val));
        to.setAmount(to.getAmount().subtract(val));
        accountPort.save(from);
        accountPort.save(to);
    }
}
