package com.ment.chat.client.exception;

import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.CompletionNotFoundException;
import com.ment.chat.client.domain.exception.PromptNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.net.URISyntaxException;

@RestControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {
    public static final String TITLE_CLIENT_RAISED_EXCEPTION = "Client raised exception";
    public static final String TITLE_GENERAL_ERROR = "General error";
    public static final String TITLE_VALIDATION_ERROR = "Validation error";
    public static final String TITLE_CHAT_NOT_FOUND = "Chat not found";
    public static final String TITLE_PROMPT_NOT_FOUND = "Prompt not found";
    public static final String TITLE_COMPLETION_NOT_FOUND = "Completion not found";
    public static final String TITLE_API_ERROR = "API error";

    public static final String TYPE_PROMPT = "prompt";
    public static final String TYPE_COMPLETION = "completion";
    public static final String TYPE_CHAT = "chat";
    public static final String TYPE_CLIENT_CALL = "client-call";
    public static final String TYPE_VALIDATION = "validation";
    public static final String TYPE_LLM_CALL = "llm-call";

    public static final String PROPERTY_KEY_VIOLATIONS = "violations";

    @ExceptionHandler(PromptNotFoundException.class)
    ProblemDetail handlePromptNotFoundException(PromptNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_PROMPT_NOT_FOUND);
        problemDetail.setType(URI.create(TYPE_PROMPT));
        return problemDetail;
    }

    @ExceptionHandler(CompletionNotFoundException.class)
    ProblemDetail handlePromptNotFoundException(CompletionNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_COMPLETION_NOT_FOUND);
        problemDetail.setType(URI.create(TYPE_COMPLETION));
        return problemDetail;
    }

    @ExceptionHandler(ChatNotFoundException.class)
    ProblemDetail handleChatNotFoundException(ChatNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_CHAT_NOT_FOUND);
        problemDetail.setType(URI.create(TYPE_CHAT));
        return problemDetail;
    }

    @ExceptionHandler(WebClientException.class)
    ProblemDetail handleWebClientException(WebClientException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_GENERAL_ERROR);
        problemDetail.setType(URI.create(TYPE_CLIENT_CALL));
        return problemDetail;
    }

    @ExceptionHandler(ValidationException.class)
    ProblemDetail handleValidationException(ValidationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_VALIDATION_ERROR);
        problemDetail.setType(URI.create(TYPE_VALIDATION));
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleException(MethodArgumentNotValidException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getBody().getDetail());
        problemDetail.setTitle(TITLE_VALIDATION_ERROR);
        problemDetail.setType(URI.create(TYPE_VALIDATION));
        problemDetail.setInstance(parseURI(((ServletWebRequest) request).getRequest().getRequestURI()));
        setViolations(problemDetail, ex.getBindingResult());
        return problemDetail;
    }

    @ExceptionHandler(NestedRuntimeException.class)
    ProblemDetail handleException(NestedRuntimeException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_VALIDATION_ERROR);
        problemDetail.setInstance(parseURI(((ServletWebRequest) request).getRequest().getRequestURI()));
        problemDetail.setType(URI.create(TYPE_VALIDATION));
        return problemDetail;
    }

    //external gpt API used, needs certificate for api.openai.com
    @ExceptionHandler(RestClientException.class)
    ProblemDetail handleRestClientException(RestClientException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(TITLE_CLIENT_RAISED_EXCEPTION);
        problemDetail.setType(URI.create(TYPE_LLM_CALL));
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
        problemDetail.setTitle(TITLE_API_ERROR);
        problemDetail.setType(URI.create(TYPE_LLM_CALL));
        return problemDetail;
    }

    private URI parseURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void setViolations(ProblemDetail problemDetail, BindingResult bindingResult) {
        var violations = bindingResult.getFieldErrors().stream()
                .map(fieldError -> {
                    var violation = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation error");
                    violation.setType(URI.create(TYPE_VALIDATION));
                    violation.setProperty("field", fieldError.getField());
                    violation.setProperty("rejectedValue", fieldError.getRejectedValue());
                    violation.setProperty("message", fieldError.getDefaultMessage());
                    return violation;
                })
                .toList();
        problemDetail.setProperty(PROPERTY_KEY_VIOLATIONS, violations);
    }

}
