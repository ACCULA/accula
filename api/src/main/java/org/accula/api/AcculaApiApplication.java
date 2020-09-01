package org.accula.api;

import org.accula.api.service.CloneDetectionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;

@SpringBootApplication
@SuppressWarnings("PMD.UseUtilityClass")
public class AcculaApiApplication {
    public static void main(final String[] args) {
        final var app = new SpringApplication(AcculaApiApplication.class);
        app.addListeners((final ContextRefreshedEvent event) -> event.getApplicationContext()
                .getBean(CloneDetectionService.class)
                .fillSuffixTree().block());
        app.run(args);
    }
}
