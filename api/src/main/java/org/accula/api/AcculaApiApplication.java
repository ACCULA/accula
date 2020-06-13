package org.accula.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
public class AcculaApiApplication {
    public static void main(final String[] args) {
        ReactorDebugAgent.init();
        SpringApplication.run(AcculaApiApplication.class, args);
    }
}
