package com.xblog.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AccessTokenBlacklistedException extends ResponseStatusException {
    public AccessTokenBlacklistedException() {
        super(HttpStatus.BAD_REQUEST, "access token is blacklisted");
    }
}
