package com.nju.edu;

public class SeecoderBuildException extends RuntimeException {

    public static final SeecoderBuildException UNKNOWN_ERROR = new SeecoderBuildException("-1", "Unknown Error");

    public static final SeecoderBuildException JSON_ERROR = new SeecoderBuildException("101", "JSON Processing Error");

    public static final SeecoderBuildException IO_ERROR = new SeecoderBuildException("102", "Execute IO Error");

    public static final SeecoderBuildException UNAHTORIZATION_ERROR = new SeecoderBuildException("403", "Unauthorization Error");

    public static final SeecoderBuildException NOT_FOUND_ERROR = new SeecoderBuildException("404", "Not Found Error");

    public static final SeecoderBuildException INTERNAL_SERVER_ERROR = new SeecoderBuildException("500", "Internal Server Error");

    public static final String BIZ_ERROR_CODE = "100";

    private String code;

    private String msg;

    public SeecoderBuildException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public SeecoderBuildException(String code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}
