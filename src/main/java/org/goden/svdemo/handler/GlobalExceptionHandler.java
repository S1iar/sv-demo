package org.goden.svdemo.handler;

import org.goden.svdemo.pojo.Result;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException ex) {
        // 1. 从异常中获取所有字段错误
        StringBuilder errorMsg = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // 2. 获取每个字段的错误信息
            String message = error.getDefaultMessage();
            // 3. 拼接错误信息（可根据需要定制格式，例如带上字段名）
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                errorMsg.append(fieldName).append(": ").append(message).append("; ");
            } else {
                errorMsg.append(message).append("; ");
            }
        });

        // 4. 返回自定义的 Result 对象，状态码设为 400 等表示客户端错误
        return Result.error(errorMsg.toString());
    }
}
