package com.prototype.proxy.exception;

import lombok.Getter;

@Getter
public class ProxyException extends RuntimeException {
    private String errorCode;
    private String requestId;

    public ProxyException(String message) {
        super(message);
    }

    public ProxyException(Throwable cause) {
        super(cause);
    }

    public ProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProxyException(String message, Throwable cause, String requestId) {
        super(message, cause);
        this.requestId = requestId;
    }
}
