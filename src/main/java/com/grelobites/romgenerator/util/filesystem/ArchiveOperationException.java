package com.grelobites.romgenerator.util.filesystem;

public class ArchiveOperationException extends RuntimeException {
    private String messageKey;

    public ArchiveOperationException(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}

