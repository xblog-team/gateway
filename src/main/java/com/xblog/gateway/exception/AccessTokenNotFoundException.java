package com.xblog.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AccessTokenNotFoundException extends ResponseStatusException {
    public AccessTokenNotFoundException() {
        this("access token not found");
    }

    public AccessTokenNotFoundException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
