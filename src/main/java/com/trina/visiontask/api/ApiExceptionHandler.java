package com.trina.visiontask.api;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler
{

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiBody<Object> handleGlobalException(Exception ex)
    {
        return ApiBody.failure(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage());
    }

    @ExceptionHandler(AmqpConnectException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiBody<Object> handleGlobalException(AmqpConnectException ex)
    {
        return ApiBody.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
    }
}