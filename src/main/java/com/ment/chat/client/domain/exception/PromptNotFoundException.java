package com.ment.chat.client.domain.exception;

public class PromptNotFoundException extends RuntimeException {

    public PromptNotFoundException(String message) {
        super(message);
    }
}
