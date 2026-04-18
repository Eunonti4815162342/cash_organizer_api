package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;

public interface TransactionImpact {
    TransactionItem.TransactionType supportedType();
    void apply(TransactionItem transaction, User user);
    void revert(TransactionItem transaction, User user);
}
