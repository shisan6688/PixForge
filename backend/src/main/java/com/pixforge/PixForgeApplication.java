package com.pixforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 启动 PixForge 后端服务。 */
@SpringBootApplication
public class PixForgeApplication {
    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(PixForgeApplication.class, args);
    }
}
