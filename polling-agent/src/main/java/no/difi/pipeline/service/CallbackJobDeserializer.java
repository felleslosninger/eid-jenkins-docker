package no.difi.pipeline.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class CallbackJobDeserializer extends JsonDeserializer<CallbackJob> {

    private JobFactory jobFactory;

    public CallbackJobDeserializer(JobFactory jobFactory) {
        this.jobFactory = jobFactory;
    }

    @Override
    public CallbackJob deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        return jobFactory.callbackRequest()
                .onBehalfOf(value("onBehalfOf", node))
                .to(new URL(value("address", node)));
    }

    private String value(String key, JsonNode node) throws IOException {
        if (!node.has(key)) throw new IOException("Missing node \"" + key + "\"");
        return node.get(key).asText();
    }

}
