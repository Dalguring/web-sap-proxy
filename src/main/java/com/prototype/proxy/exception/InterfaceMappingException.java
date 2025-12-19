package com.prototype.proxy.exception;

import lombok.Getter;

@Getter
public class InterfaceMappingException extends RuntimeException {
    private final String interfaceId;

    public InterfaceMappingException(String interfaceId, String message) {
        super(message);
        this.interfaceId = interfaceId;
    }

}
