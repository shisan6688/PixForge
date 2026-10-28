package com.pixforge.api;

/** 表示资源不存在或当前用户无权访问。 */
public class ResourceUnavailableException extends RuntimeException {
    /** 使用对外显示的资源不可用原因创建异常。 */
    public ResourceUnavailableException(String message) { super(message); }
}
