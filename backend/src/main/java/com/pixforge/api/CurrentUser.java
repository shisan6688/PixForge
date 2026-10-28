package com.pixforge.api;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/** 从演示会话解析当前用户身份。 */
@Component
public class CurrentUser {
    /** 返回会话对应的演示用户数据库 ID。 */
    public long id(HttpSession session) { return "B".equals(session.getAttribute("demoKey")) ? 2L : 1L; }
}
