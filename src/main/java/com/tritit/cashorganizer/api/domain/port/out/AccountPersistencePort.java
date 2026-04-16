package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface AccountPersistencePort {
    List<AccountItem> findAllByUser(User user);
    Optional<AccountItem> findById(Long id);
    AccountItem save(AccountItem account);
    void delete(Long id);
}
