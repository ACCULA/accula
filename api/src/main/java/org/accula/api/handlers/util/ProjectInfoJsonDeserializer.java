package org.accula.api.handlers.util;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class ProjectInfoJsonDeserializer {

    private ProjectInfoJsonDeserializer() {}

    public static Flux<Tuple2<String, Object>> deserialize(Mono<JsonNode> githubInfoJson) {
        return Flux.merge(
                githubInfoJson.map(jsonNode -> Tuples.of("url", jsonNode
                        .get("owner")
                        .get("url")
                        .asText())),
                githubInfoJson.map(jsonNode -> Tuples.of("owner", jsonNode
                        .get("owner")
                        .get("login")
                        .asText())),
                githubInfoJson.map(jsonNode -> Tuples.of("repoName", jsonNode
                        .get("name")
                        .asText())),
                githubInfoJson.map(jsonNode -> Tuples.of("description", jsonNode
                        .get("description")
                        .asText())),
                githubInfoJson.map(jsonNode -> Tuples.of("avatar", jsonNode
                        .get("owner")
                        .get("avatar_url")
                        .asText()))
        );
    }

}
