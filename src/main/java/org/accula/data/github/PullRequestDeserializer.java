package org.accula.data.github;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.Instant;

public class PullRequestDeserializer extends JsonDeserializer<PullRequest> {
    @Override
    public PullRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = p.getCodec().readTree(p);
        var id = jsonNode.get("id").asLong();
        var number = jsonNode.get("number").asInt();
        var title = jsonNode.get("title").textValue();
        var created = Instant.parse(jsonNode.get("created_at").textValue());
        var userName = jsonNode.get("user").get("login").textValue();
        var linkToRepoNode = jsonNode.get("head").get("repo").get("contents_url");
        var linkToRepoContent = !(linkToRepoNode == null || linkToRepoNode.isNull()) ?
                linkToRepoNode.textValue().replace("{+path}", "") : null;

        return new PullRequest(
                id,
                number,
                title,
                created,
                userName,
                linkToRepoContent
        );
    }
}
