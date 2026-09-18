package com.doctrip.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling} backs FR-03's Admin-SLA auto-takeover (see
 * {@code service.AdminSlaEnforcer}) — disabled under the {@code test} profile so scheduled runs
 * don't race test data (see that class's {@code @Profile("!test")}). */
@SpringBootApplication
@EnableScheduling
public class DoctripBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctripBackendApplication.class, args);
    }
}
