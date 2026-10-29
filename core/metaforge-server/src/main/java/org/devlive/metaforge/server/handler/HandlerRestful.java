package org.devlive.metaforge.server.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.authorize.UserNotEqualsException;
import org.devlive.metaforge.service.SelfException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class HandlerRestful
{
    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<List<HandlerField>> handlerMethodArgumentNotValidException(MethodArgumentNotValidException ex)
    {
        List<HandlerField> list = new ArrayList<>();
        BindingResult bindingResult = ex.getBindingResult();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            HandlerField field = new HandlerField();
            field.setField(fieldError.getField());
            field.setMessage(fieldError.getDefaultMessage());
            list.add(field);
        }
        return CommonResponse.failure(ServiceState.REQUEST_VALID_ARGUMENT, list);
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<String> handlerConstraintViolationException(DataIntegrityViolationException ex)
    {
        return CommonResponse.failure(ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<String> handlerConstraintViolationException(HttpMessageNotReadableException ex)
    {
        log.error("", ex);
        if (ex.getCause() instanceof InvalidFormatException) {
            return CommonResponse.failure(ServiceState.REQUEST_VALID_ARGUMENT_FORMAT);
        }
        else {
            return CommonResponse.failure(ServiceState.REQUEST_VALID_ARGUMENT_LAYOUT);
        }
    }

    @ExceptionHandler({UserNotEqualsException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<String> handlerUserNotEqualsException(UserNotEqualsException ex)
    {
        return CommonResponse.failure(ServiceState.USER_UNAUTHORIZED);
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<Object> handlerNoHandlerFoundException(NoHandlerFoundException ex)
    {
        log.error("", ex);
        return CommonResponse.failure(ServiceState.REQUEST_EXCEPTION, ex.getMessage());
    }

    @ExceptionHandler({SelfException.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<Object> handlerSelfException(SelfException ex)
    {
        return CommonResponse.failure(ServiceState.USER_UNAUTHORIZED);
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public CommonResponse<Object> handlerException(Exception ex)
    {
        log.error("", ex);
        return CommonResponse.failure(ServiceState.REQUEST_EXCEPTION, ex.getMessage());
    }
}
