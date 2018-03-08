package no.difi.pipeline.service;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static jdk.incubator.http.HttpResponse.BodyHandler.asString;

public class JiraStatusJob implements Job {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private URL address;
    private URL callbackAddress;
    private String issue;
    private String positiveTargetStatus;
    private String negativeTargetStatus;
    private HttpClient httpClient;
    private PollQueue pollQueue;
    private JobFactory jobFactory;
    private JobRepository jobRepository;
    private HttpRequest request;
    private String username;
    private String password;

    public URL getAddress() {
        return address;
    }

    public URL getCallbackAddress() {
        return callbackAddress;
    }

    public String getIssue() {
        return issue;
    }

    public String getPositiveTargetStatus() {
        return positiveTargetStatus;
    }

    public String getNegativeTargetStatus() {
        return negativeTargetStatus;
    }

    @Override
    public void execute() {
        try {
            HttpResponse<String> response = httpClient.send(request, asString());
            if (!ok(response)) {
                logger.warn("{}: HTTP status {}", request.uri(), response.statusCode());
                newPollIn(60);
                return;
            } else if (positiveTargetStatus != null && !issueStatus(response).equals(positiveTargetStatus)) {
                logger.info("{}: Issue status is not {} (it is {})", request.uri(), positiveTargetStatus, issueStatus(response));
                newPollIn(10);
                return;
            } else if (negativeTargetStatus != null && issueStatus(response).equals(negativeTargetStatus)) {
                logger.info("{}: Issue status is still {}", request.uri(), issueStatus(response));
                newPollIn(10);
                return;
            }
        } catch (Exception e) {
            logger.warn("{}: Failed: {}", request.uri(), e.getMessage(), e);
            newPollIn(60);
            return;
        }
        if (positiveTargetStatus != null) {
            logger.info("Polling Jira for status equal to {} on issue {} succeeded", positiveTargetStatus, issue);
        } else if (negativeTargetStatus != null) {
            logger.info("Polling Jira for status different than {} on issue {} succeeded", positiveTargetStatus, issue);
        }
        Job callbackJob = jobFactory.callbackRequest().onBehalfOf(id()).to(callbackAddress);
        pollQueue.add(callbackJob, now());
        jobRepository.save(callbackJob);
        jobRepository.delete(this.id());
    }

    private void newPollIn(int seconds) {
        pollQueue.add(this, now().plus(Duration.ofSeconds(seconds)));
    }

    private HttpRequest request() {
        URI uri = URI.create(format("%s/rest/api/2/issue/%s?fields=status", address, issue));
        return HttpRequest.newBuilder(uri).GET().setHeader("Authorization", "Basic " + auth()).build();
    }

    private String auth() {
        try {
            return Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String issueStatus(HttpResponse<String> response) {
        if (response.body().isEmpty())
            throw new RuntimeException("Jira response is empty");
        JsonValue jsonResponse = Json.createReader(new StringReader(response.body())).readValue();
        if (jsonResponse.getValueType() != JsonValue.ValueType.OBJECT)
            throw new RuntimeException("Jira response contains no Json object");
        return jsonResponse.asJsonObject().getJsonObject("fields").getJsonObject("status").getString("id");
    }

    private boolean ok(HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    @Override
    public String id() {
        return getClass().getSimpleName() + "-"
                + ((positiveTargetStatus != null) ? positiveTargetStatus : "not" + negativeTargetStatus)
                + "-" + issue;
    }

    public interface JiraAddress {
        IssueId to(URL address);
    }

    public interface IssueId {
        Status getStatusForIssue(String issueId);
    }

    public interface Status {
        CallbackAddress andExpectStatusEqualTo(String status);
        CallbackAddress andExpectStatusNotEqualTo(String status);
    }

    public interface CallbackAddress {
        JiraStatusJob andPostWhenReadyTo(URL callbackAddress);
    }

    public static class Builder implements JiraAddress, IssueId, Status, CallbackAddress {

        private JiraStatusJob instance = new JiraStatusJob();

        public Builder(HttpClient httpClient, String username, String password, PollQueue pollQueue, JobFactory jobFactory, JobRepository jobRepository) {
            instance.httpClient = httpClient;
            instance.username = username;
            instance.password = password;
            instance.pollQueue = pollQueue;
            instance.jobFactory = jobFactory;
            instance.jobRepository = jobRepository;
        }

        @Override
        public IssueId to(URL address) {
            instance.address = address;
            return this;
        }

        @Override
        public Status getStatusForIssue(String issueId) {
            instance.issue = issueId;
            return this;
        }

        @Override
        public CallbackAddress andExpectStatusEqualTo(String status) {
            instance.positiveTargetStatus = status;
            return this;
        }

        @Override
        public CallbackAddress andExpectStatusNotEqualTo(String status) {
            instance.negativeTargetStatus = status;
            return this;
        }

        @Override
        public JiraStatusJob andPostWhenReadyTo(URL callbackAddress) {
            instance.callbackAddress = callbackAddress;
            instance.request = instance.request();
            return instance;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JiraStatusJob that = (JiraStatusJob) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id());
    }
}
