package com.pixforge.api;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public long id(HttpSession session) { return "B".equals(session.getAttribute("demoKey")) ? 2L : 1L; }
}
