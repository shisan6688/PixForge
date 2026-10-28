package com.pixforge.api;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 读取和切换演示用户会话。 */
@RestController
@RequestMapping("/api/demo/session")
public class DemoSessionController {
    /** 对外展示的演示用户信息。 */
    public record DemoUser(String id, String demoKey, String displayName) {}
    /** 演示用户切换请求，仅允许 A 或 B。 */
    public record SessionChoice(@Pattern(regexp = "A|B") String demoKey) {}

    /** 返回当前会话的演示用户。 */
    @GetMapping
    public DemoUser current(HttpSession session) {
        return user((String) session.getAttribute("demoKey"));
    }

    /** 将会话切换到指定的演示用户。 */
    @PostMapping
    public DemoUser switchUser(@RequestBody SessionChoice choice, HttpSession session) {
        if (choice.demoKey() == null || !choice.demoKey().matches("A|B")) throw new IllegalArgumentException("请选择演示用户 A 或 B");
        session.setAttribute("demoKey", choice.demoKey());
        return user(choice.demoKey());
    }

    /** 构造演示用户的响应信息。 */
    private DemoUser user(String key) {
        return "B".equals(key) ? new DemoUser("2", "B", "演示用户 B") : new DemoUser("1", "A", "演示用户 A");
    }
}
