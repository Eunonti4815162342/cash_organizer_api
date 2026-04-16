package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.User;
import java.util.Optional;

public interface UserPersistencePort {
    Optional<User> findByEmail(String email);
}
