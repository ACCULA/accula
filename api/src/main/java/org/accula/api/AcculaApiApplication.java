package org.accula.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings("PMD.UseUtilityClass")
public class AcculaApiApplication {
    public static void main(final String[] args) {
        SpringApplication.run(AcculaApiApplication.class, args);
    }
}
