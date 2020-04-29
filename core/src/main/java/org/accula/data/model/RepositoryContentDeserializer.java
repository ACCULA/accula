package org.accula.data.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class RepositoryContentDeserializer extends JsonDeserializer<RepositoryContent> {
    @Override
    public RepositoryContent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = p.getCodec().readTree(p);
        var path = jsonNode.get("filename").textValue();
        var name = path.substring(path.lastIndexOf('/') + 1);
        var url = jsonNode.get("raw_url").textValue();
        var linkToFile = jsonNode.get("blob_url").textValue();

        return new RepositoryContent(name, path, url, linkToFile);
    }
}
