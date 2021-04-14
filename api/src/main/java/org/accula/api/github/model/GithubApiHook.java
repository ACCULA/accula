package org.accula.api.github.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import java.util.Locale;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static org.accula.api.github.model.GithubApiHook.Config.Insecurity.NO;
import static org.accula.api.github.model.GithubApiHook.Config.Insecurity.YES;
import static org.accula.api.github.model.GithubApiHook.Event.PULL_REQUEST;

/**
 * @author Anton Lamtev
 * @author Vadim Dyachkov
 */
@JsonAutoDetect(fieldVisibility = ANY)
@Value
public class GithubApiHook {
    String name = "web";
    Event[] events;
    Boolean active;
    Config config;

    public static GithubApiHook onPullUpdates(final String url, final String secret, final boolean isSslEnabled) {
        return new GithubApiHook(new Event[]{PULL_REQUEST}, true, new Config(url, secret, isSslEnabled ? NO : YES));
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

    @JsonAutoDetect(fieldVisibility = ANY)
    @Value
    public static class Config {
        @JsonProperty("url")
        String callbackUrl;
        @JsonProperty("content_type")
        String contentType = "json";
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
