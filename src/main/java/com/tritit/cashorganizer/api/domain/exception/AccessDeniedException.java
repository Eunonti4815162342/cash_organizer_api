package com.tritit.cashorganizer.api.domain.exception;

public class AccessDeniedException extends DomainException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
