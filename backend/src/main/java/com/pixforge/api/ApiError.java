package com.pixforge.api;

/** 向客户端返回统一的错误代码与提示。 */
public record ApiError(String code, String message) {}
