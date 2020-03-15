package org.accula.api.config;

import org.accula.api.handlers.GreetingHandler;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class HandlersConfig {
    @Bean
    @NotNull
    public GreetingHandler greetingHandler() {
        return new GreetingHandler();
    }
}
