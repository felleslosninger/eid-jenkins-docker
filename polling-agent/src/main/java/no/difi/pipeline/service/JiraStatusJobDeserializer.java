package no.difi.pipeline.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class JiraStatusJobDeserializer extends JsonDeserializer<JiraStatusJob> {

    private JobFactory jobFactory;

    public JiraStatusJobDeserializer(JobFactory jobFactory) {
        this.jobFactory = jobFactory;
    }

    @Override
    public JiraStatusJob deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node.has("positiveTargetStatus")) {
            return jobFactory.jiraRequest()
                    .to(address(node))
                    .getStatusForIssue(issue(node))
                    .andExpectStatusEqualTo(value("positiveTargetStatus", node))
                    .andPostWhenReadyTo(callbackAddress(node));
        } else {
            return jobFactory.jiraRequest()
                    .to(address(node))
                    .getStatusForIssue(issue(node))
                    .andExpectStatusNotEqualTo(value("negativeTargetStatus", node))
                    .andPostWhenReadyTo(callbackAddress(node));
        }
    }

    private URL address(JsonNode node) throws IOException {
        return url("address", node);
    }

    private String issue(JsonNode node) throws IOException {
        return value("issue", node);
    }

    private URL callbackAddress(JsonNode node) throws IOException {
        return url("callbackAddress", node);
    }

    private URL url(String key, JsonNode node) throws IOException {
        return new URL(value(key, node));
    }

    private String value(String key, JsonNode node) throws IOException {
        if (!node.has(key)) throw new IOException("Missing node \"" + key + "\"");
        return node.get(key).asText();
    }

}
