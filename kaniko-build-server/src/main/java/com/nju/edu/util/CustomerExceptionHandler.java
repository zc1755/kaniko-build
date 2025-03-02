package com.nju.edu.util;

import com.nju.edu.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;


@RestControllerAdvice
public class CustomerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomerExceptionHandler.class);

    @ExceptionHandler(value = ServiceException.class)
    public Response serviceExceptionHandler(HttpServletRequest req, ServiceException e) {
        e.printStackTrace();
        logger.error("Error: ", e);
        return Response.buildFailure(e);
    }

    @ExceptionHandler(value = Exception.class)
    public Response exceptionHandler(HttpServletRequest req, Exception e){
        e.printStackTrace();
        logger.error("Error: ", e);
        return Response.buildFailure(e);
    }
}
