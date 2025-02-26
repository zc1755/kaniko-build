package com.nju.edu.util;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    public static final ServiceException INVALID_DATA = new ServiceException("400", "不合法的数据");

    public static final ServiceException BAD_REQUEST = new ServiceException("400", "不合法的请求");

    private String code;

    private String msg;

    public ServiceException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public ServiceException(String code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}
