package com.nju.edu.model;

import com.nju.edu.util.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {

    private String code;

    private String msg;

    private Object result;

    public static Response buildSuccess(Object result) {
        return new Response("0", "", result);
    }

    public static  Response buildFailure(ServiceException e) {
        return new Response(e.getCode(), e.getMsg(), null);
    }

    public static  Response buildFailure(Exception e) {
        return new Response("-1", e.getLocalizedMessage(), null);
    }

    public static Response buildFailure(String code, String message) {
        return new Response(code, message, null);
    }

    public static Response buildFailure(String message) {
        return new Response("-1", message, null);
    }
}
