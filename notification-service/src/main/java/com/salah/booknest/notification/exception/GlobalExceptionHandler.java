package com.salah.booknest.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // a browser closed its live notification stream
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void clientDisconnected() {
        log.debug("Client disconnected from a streaming response");
    }
}
