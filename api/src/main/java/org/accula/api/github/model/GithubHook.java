package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import java.util.Locale;

import static org.accula.api.github.model.GithubHook.Config.Insecurity.YES;
import static org.accula.api.github.model.GithubHook.Event.PULL_REQUEST;

@Value
public class GithubHook {
    String name = "web";
    Event[] events;
    Boolean active;
    Config config;

    public static GithubHook onPull(final String url, final String secret) {
        return new GithubHook(new Event[]{PULL_REQUEST}, true, new Config(url, secret, YES));
    }

    public enum Event {
        PULL_REQUEST,
        PUSH,
        ;

        @JsonValue
        public String value() {
            return name().toLowerCase(Locale.US);
        }
    }

    @Value
    public static class Config {
        @JsonProperty("url")
        String callbackUrl;
        @JsonProperty("content_type")
        String contentType = "application/json";
        String secret;
        @JsonProperty("insecure_ssl")
        Insecurity insecure;

        public enum Insecurity {
            YES,
            NO,
            ;

            @JsonValue
            public String value() {
                return this == YES ? "1" : "0";
            }
        }
    }
}
