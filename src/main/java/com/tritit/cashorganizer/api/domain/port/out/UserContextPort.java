package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.User;

public interface UserContextPort {
    User getCurrentUser();
}
