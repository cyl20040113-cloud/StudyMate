package com.cyl.studymate.common.exception;

import com.cyl.studymate.common.BaseResponse;
import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(
            BusinessException e) {

        //1. 业务异常属于可以预料的异常
        //只记录 warn，不打印完整异常堆栈
        log.warn(
                "businessException: code = {}, message = {}, description = {}",
                e.getCode(),
                e.getMessage(),
                e.getDescription()
        );

        return ResultUtils.error(
                e.getCode(),
                e.getMessage(),
                e.getDescription()
        );
    }

    /**
     * 请求参数异常处理
     *
     * 主要处理：
     * 1. JSON 格式错误
     * 2. 缺少请求参数
     * 3. 请求参数类型错误
     * 4. 参数绑定错误
     * 5. 参数校验错误
     *
     * 这些异常本质上属于客户端请求错误，
     * 不应该返回 SYSTEM_ERROR（50000）
     *
     * @param e
     * @return
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class,
            MethodArgumentNotValidException.class
    })
    public BaseResponse<?> requestParamExceptionHandler(Exception e) {

        //2. 请求参数异常属于用户请求错误
        //一般不需要打印完整异常堆栈
        log.warn(
                "requestParamException: type = {}, message = {}",
                e.getClass().getSimpleName(),
                e.getMessage()
        );

        return ResultUtils.error(ErrorCode.PARAMS_ERROR);
    }

    /**
     * 系统异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> runtimeExceptionHandler(Exception e) {

        //3. 未知系统异常需要打印完整异常堆栈
        //方便定位真正的程序 Bug
        log.error("systemException", e);

        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}