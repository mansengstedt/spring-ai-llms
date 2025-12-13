package com.ment.chat.client.domain.exception;

public class CompletionNotFoundException extends RuntimeException {

    public CompletionNotFoundException(String message) {
        super(message);
    }
}
