package com.tcm.platform.common;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 统一 API 响应封装类
 * 
 * 任务：
 * 1. 使用 Lombok @Data 注解（已添加）
 * 2. 定义以下字段：code(int), message(String), data(T), timestamp(LocalDateTime)
 * 3. 实现 4 个静态工厂方法：
 *    - success(T data)          → code=200, message="success"
 *    - success(String msg, T data) → code=200, 自定义 message
 *    - error(int code, String msg) → 错误响应
 *    - error(String msg)         → code=500
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    /**
     * 成功响应（仅数据）
     */
    public static <T> Result<T> success(T data) {
        return success("success", data);
    }
    
    /**
     * 成功响应（自定义消息 + 数据）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    /**
     * 错误响应（指定错误码和消息）
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    /**
     * 错误响应（默认 500）
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
