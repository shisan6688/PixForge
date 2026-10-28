package com.pixforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/** The database connection is activated when persistence work begins. */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class PixForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PixForgeApplication.class, args);
    }
}
