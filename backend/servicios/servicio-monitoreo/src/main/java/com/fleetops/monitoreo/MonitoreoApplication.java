package com.fleetops.monitoreo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MonitoreoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitoreoApplication.class, args);
    }
}
