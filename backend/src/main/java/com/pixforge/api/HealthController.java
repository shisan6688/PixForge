package com.pixforge.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供服务健康检查接口。 */
@RestController
public class HealthController {
    /** 返回后端服务的存活状态。 */
    @GetMapping("/api/health")
    public Map<String, String> health() { return Map.of("status", "UP", "service", "pixforge-backend"); }
}
