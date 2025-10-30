package com.ment.chat.client.event;

import com.ment.chat.client.domain.Request;
import com.ment.chat.client.domain.Response;
import com.ment.chat.client.domain.repository.RequestRepository;
import com.ment.chat.client.domain.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpringEventListener {

    private final RequestRepository requestRepository;

    private final ResponseRepository responseRepository;

    @EventListener
    public void handleRequest(Request request) {
        requestRepository.findById(request.getRequestId())
                .ifPresentOrElse(
                        existingRequest -> log.info("Request with ID {} saved.", existingRequest.getRequestId()),
                        () -> log.info("Request with ID {} not saved yet.", request.getRequestId())
                );
    }

    @EventListener
    public void handleResponse(Response response) {
        responseRepository.findById(response.getResponseId())
                .ifPresentOrElse(
                        existingResponse -> log.info("Response with ID {} answering request {} saved.", existingResponse.getResponseId(), existingResponse.getRequestId()),
                        () -> log.info("Response with ID {} not saved yet.", response.getResponseId())
                );
    }
}
