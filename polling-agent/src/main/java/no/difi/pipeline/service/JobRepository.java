package no.difi.pipeline.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Repository
public class JobRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final File directory;
    private final JobFactory jobFactory;
    private final ObjectMapper objectMapper;

    public JobRepository(File repositoryDirectory, JobFactory jobFactory) {
        this.directory = repositoryDirectory;
        this.jobFactory = jobFactory;
        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(JiraStatusJob.class, new JiraStatusJobDeserializer());
        module.addDeserializer(CallbackJob.class, new CallbackJobDeserializer());
        objectMapper.registerModule(module);
        if (!repositoryDirectory.exists()) {
            if (!repositoryDirectory.mkdirs())
                throw new RuntimeException("Failed to create repository directory " + repositoryDirectory);
        }
    }

    public void save(Job job) {
        try {
            objectMapper.writer().writeValue(file(job.id()), job);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save job " + job.id() + ": " + e.getMessage());
        }
        logger.info("Saved " + file(job.id()));
    }

    public void delete(String jobId) {
        if (!file(jobId).delete()) {
            logger.warn("Failed to delete " + file(jobId));
        } else {
            logger.info("Deleted " + file(jobId));
        }
    }

    public List<Job> load() {
        return files().stream()
                .map(this::parseFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    private File file(String jobId) {
        return new File(directory, jobId);
    }

    private List<File> files() {
        File[] files = directory.listFiles();
        if (files == null) throw new RuntimeException("Failed to list files (returned null)");
        return Arrays.asList(files);
    }

    private Optional<Job> parseFile(File file) {
        logger.info("Loading file " + file.getName() + "...");
        try {
            if (file.getName().startsWith(JiraStatusJob.class.getSimpleName()))
                return Optional.of(objectMapper.readerFor(JiraStatusJob.class).withAttribute("fileName", file.getName()).readValue(file));
            else if (file.getName().startsWith(CallbackJob.class.getSimpleName()))
                return Optional.of(objectMapper.readerFor(CallbackJob.class).withAttribute("fileName", file.getName()).readValue(file));
            else
                logger.warn("Unrecognized file name for a job: " + file.getName());
        } catch (IOException e) {
            logger.warn("Failed to parse file " + file.getName() + ": " + e.toString());
        }
        return Optional.empty();
    }

    private class CallbackJobDeserializer extends JsonDeserializer<CallbackJob> {

        @Override
        public CallbackJob deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.getCodec().readTree(parser);
            return jobFactory.callbackRequest()
                    .id((String)context.getAttribute("fileName"))
                    .onBehalfOf(value("onBehalfOf", node))
                    .to(new URL(value("address", node)));
        }

    }

    private class JiraStatusJobDeserializer extends JsonDeserializer<JiraStatusJob> {

        @Override
        public JiraStatusJob deserialize(JsonParser parser, DeserializationContext context) throws IOException {

            JsonNode node = parser.getCodec().readTree(parser);
            if (node.hasNonNull("positiveTargetStatus")) {
                return jobFactory.jiraRequest()
                        .id((String)context.getAttribute("fileName"))
                        .to(address(node))
                        .getStatusForIssues(issues(node))
                        .andExpectStatusEqualTo(value("positiveTargetStatus", node))
                        .andPostWhenReadyTo(callbackAddress(node));
            } else {
                return jobFactory.jiraRequest()
                        .id((String)context.getAttribute("fileName"))
                        .to(address(node))
                        .getStatusForIssues(issues(node))
                        .andExpectStatusNotEqualTo(value("negativeTargetStatus", node))
                        .andPostWhenReadyTo(callbackAddress(node));
            }
        }

        private URL address(JsonNode node) throws IOException {
            return url("address", node);
        }

        private List<String> issues(JsonNode node) throws IOException {
            if (node.has("issue"))
                // Backwards compatible
                return singletonList(value("issue", node));
            else
                return values("issues", node);
        }

        private URL callbackAddress(JsonNode node) throws IOException {
            return url("callbackAddress", node);
        }

        private URL url(String key, JsonNode node) throws IOException {
            return new URL(value(key, node));
        }

    }

    private static String value(String key, JsonNode node) throws IOException {
        if (!node.has(key)) throw new IOException("Missing node \"" + key + "\"");
        return node.get(key).asText();
    }

    private static List<String> values(String key, JsonNode node) throws IOException {
        if (!node.has(key)) throw new IOException("Missing node \"" + key + "\"");
        return node.findValues(key).stream().map(e -> e.get(0).asText()).collect(toList());
    }

}
