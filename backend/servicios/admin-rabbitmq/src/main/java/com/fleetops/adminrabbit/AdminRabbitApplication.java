package com.fleetops.adminrabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminRabbitApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminRabbitApplication.class, args);
    }
}
