package org.accula.data.github;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class RepositoryContentDeserializer extends JsonDeserializer<RepositoryFile> {
    @Override
    public RepositoryFile deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = p.getCodec().readTree(p);
        var path = jsonNode.get("filename").textValue();
        var name = path.substring(path.lastIndexOf('/') + 1);
        var url = jsonNode.get("raw_url").textValue();
        var linkToFile = jsonNode.get("blob_url").textValue();

        return new RepositoryFile(name, path, url, linkToFile);
    }
}
