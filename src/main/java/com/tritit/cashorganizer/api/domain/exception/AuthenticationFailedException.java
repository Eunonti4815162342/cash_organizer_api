package com.tritit.cashorganizer.api.domain.exception;

public class AuthenticationFailedException extends DomainException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
