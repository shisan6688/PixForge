package com.pixforge.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 将服务异常转换为稳定的 HTTP 错误响应。 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    /** 将不可访问的资源映射为 404。 */
    @ExceptionHandler(ResourceUnavailableException.class)
    public ResponseEntity<ApiError> unavailable(ResourceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", exception.getMessage()));
    }

    /** 将非法业务参数映射为 400。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", exception.getMessage()));
    }

    /** 报告超过上传大小限制的文件。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ApiError("FILE_TOO_LARGE", "图片不能超过 5 MB"));
    }

    /** 报告无法解析的请求内容。 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class,
                       MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> malformed(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "请求格式或必填字段无效"));
    }

    /** 返回字段校验失败的详细信息。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", "请求参数无效",
            "fields", exception.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(
                error -> error.getField(), error -> error.getDefaultMessage() == null ? "参数无效" : error.getDefaultMessage(), (first, second) -> first))));
    }

    /** 记录并隐藏未预期的服务端异常。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        LOG.error("API request failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
    }
}
