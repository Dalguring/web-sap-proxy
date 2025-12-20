package com.prototype.proxy.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    private final String resource;

    public NotFoundException(String resource, String message) {
        super(message);
        this.resource = resource;
    }

}
