package com.ment.chat.client.exception;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {
    public static final String CLIENT_RAISED_EXCEPTION = "Client raised exception";

    @ExceptionHandler(WebClientException.class)
    ProblemDetail handleWebClientException(WebClientException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle("General error");
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
        problemDetail.setTitle(CLIENT_RAISED_EXCEPTION);
        return problemDetail;
    }


}
