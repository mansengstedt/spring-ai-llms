package com.ment.chat.client.exception;

import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.RequestNotFoundException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {
    public static final String CLIENT_RAISED_EXCEPTION = "Client raised exception";
    public static final String GENERAL_ERROR = "General error";
    public static final String CHAT_NOT_FOUND = "Chat not found";
    public static final String REQUEST_NOT_FOUND = "Request not found";
    public static final String API_ERROR = "API error";

    @ExceptionHandler(RequestNotFoundException.class)
    ProblemDetail handleRequestNotFoundException(RequestNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(REQUEST_NOT_FOUND);
        problemDetail.setType(URI.create("request"));
        return problemDetail;
    }

    @ExceptionHandler(ChatNotFoundException.class)
    ProblemDetail handleRequestNotFoundException(ChatNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(CHAT_NOT_FOUND);
        problemDetail.setType(URI.create("chat"));
        return problemDetail;
    }

    @ExceptionHandler(WebClientException.class)
    ProblemDetail handleWebClientException(WebClientException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(GENERAL_ERROR);
        return problemDetail;
    }

    //external gpt API used, needs certificate for api.openai.com
    @ExceptionHandler(RestClientException.class)
    ProblemDetail handleRestClientException(RestClientException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(CLIENT_RAISED_EXCEPTION);
        return problemDetail;
    }


    //org.springframework.ai.retry.NonTransientAiException: 429 - {
    //    "error": {
    //        "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, read the docs: https://platform.openai.com/docs/guides/error-codes/api-errors.",
    //        "type": "insufficient_quota",
    //        "param": null,
    //        "code": "insufficient_quota"
    //    }
    //}
    @ExceptionHandler(NonTransientAiException.class)
    ProblemDetail handleNonTransientAiException(NonTransientAiException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(API_ERROR);
        problemDetail.setType(URI.create("llm call"));
        return problemDetail;
    }


}
