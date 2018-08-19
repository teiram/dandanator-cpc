package com.grelobites.romgenerator.util.emulator;

public class ExecutionForbiddenException extends RuntimeException {
    public ExecutionForbiddenException() {
    }

    public ExecutionForbiddenException(String message) {
        super(message);
    }

    public ExecutionForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
