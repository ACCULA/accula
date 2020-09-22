package org.accula.api;

import org.accula.api.startup.ApplicationStartupHookInstaller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AcculaApiApplication {
    public static void main(final String[] args) {
        final var app = new SpringApplication(AcculaApiApplication.class);
        ApplicationStartupHookInstaller.installInto(app);
        app.run(args);
    }
}
