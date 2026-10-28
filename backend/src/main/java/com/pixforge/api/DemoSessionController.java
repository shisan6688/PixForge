package com.pixforge.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo/session")
public class DemoSessionController {
    public record DemoUser(String id, String demoKey, String displayName) {}
    public record SessionChoice(@Pattern(regexp = "A|B") String demoKey) {}

    @GetMapping
    public DemoUser current(HttpSession session) {
        return user((String) session.getAttribute("demoKey"));
    }

    @PostMapping
    public DemoUser switchUser(@RequestBody SessionChoice choice, HttpSession session) {
        if (choice.demoKey() == null || !choice.demoKey().matches("A|B")) throw new IllegalArgumentException("请选择演示用户 A 或 B");
        session.setAttribute("demoKey", choice.demoKey());
        return user(choice.demoKey());
    }

    private DemoUser user(String key) {
        return "B".equals(key) ? new DemoUser("2", "B", "演示用户 B") : new DemoUser("1", "A", "演示用户 A");
    }
}
