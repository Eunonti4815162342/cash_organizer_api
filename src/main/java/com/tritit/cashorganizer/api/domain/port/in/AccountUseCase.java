package com.tritit.cashorganizer.api.domain.port.in;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import java.util.List;

public interface AccountUseCase {
    List<AccountItem> getAllActiveAccounts();
    AccountItem updateAccount(Long id, AccountItem accountDetails);
    void closeAccount(Long id);
    void permanentlyDeleteAccount(Long id);
    AccountItem createAccount(AccountItem accountItem);
}
