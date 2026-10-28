package com.cyl.studymate.common;

/**
 * 返回工具类
 */
public class ResultUtils {

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }


    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }


    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(int code, String msg, String description) {
        return new BaseResponse<>(code, null, msg, description);
    }


    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String msg, String description) {
        return new BaseResponse<>(
                errorCode.getCode(),
                null,
                msg,
                description
        );
    }


    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String description) {
        return new BaseResponse<>(
                errorCode.getCode(),
                null,
                errorCode.getMsg(),
                description
        );
    }
}
