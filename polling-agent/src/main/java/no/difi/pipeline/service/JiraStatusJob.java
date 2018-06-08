package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class JiraStatusJob implements Job {

    private transient Logger logger = LoggerFactory.getLogger(getClass());
    private String id;
    private URL address;
    private URL callbackAddress;
    private List<String> issues;
    private String positiveTargetStatus;
    private String negativeTargetStatus;
    private transient JiraClient jiraClient;
    private transient PollQueue pollQueue;
    private transient JobFactory jobFactory;
    private transient JobRepository jobRepository;

    @SuppressWarnings("unused")
    public URL getAddress() {
        return address;
    }

    @SuppressWarnings("unused")
    public URL getCallbackAddress() {
        return callbackAddress;
    }

    @SuppressWarnings("unused")
    public List<String> getIssues() {
        return issues;
    }

    @SuppressWarnings("unused")
    public String getPositiveTargetStatus() {
        return positiveTargetStatus;
    }

    @SuppressWarnings("unused")
    public String getNegativeTargetStatus() {
        return negativeTargetStatus;
    }

    @Override
    public void execute() {
        JiraClient.Response response = jiraClient.requestIssueStatus(address, issues);
        if (!response.ok()) {
            logger.warn("Requesting statuses for issues {} failed: {}", issues, response.errorDetails());
            newPollIn(60);
        } else if (positiveTargetStatus != null) {
            Map<String, String> issueStatuses = response.issueStatuses();
            if (!issueStatuses.entrySet().stream().map(Map.Entry::getValue).allMatch(status -> status.equals(positiveTargetStatus))) {
                logger.info("Statuses for issues {} are not yet {} (they are {}) [{} ms]", issues, positiveTargetStatus, issueStatuses, response.requestDuration());
                newPollIn(10);
            } else {
                logger.info("Success - statuses for issues {} are now {} [{} ms]", issues, issueStatuses, response.requestDuration());
                newCallbackJob();
            }
        } else if (negativeTargetStatus != null) {
            Map<String, String> issueStatuses = response.issueStatuses();
            if (issueStatuses.entrySet().stream().map(Map.Entry::getValue).anyMatch(status -> status.equals(negativeTargetStatus))) {
                logger.info("Statuses for issues {} are still {}) [{} ms]", issues, negativeTargetStatus, response.requestDuration());
                newPollIn(10);
            } else {
                logger.info("Success - statuses for issues {} are now not {} (they are {}) [{} ms]", issues, negativeTargetStatus, issueStatuses, response.requestDuration());
                newCallbackJob();
            }
        } else {
            throw new IllegalStateException("No target status defined");
        }
    }

    private void newCallbackJob() {
        Job callbackJob = jobFactory.callbackRequest()
                .id(format("%s-%s", CallbackJob.class.getSimpleName(), UUID.randomUUID()))
                .onBehalfOf(id()).to(callbackAddress);
        pollQueue.add(callbackJob, 0);
        jobRepository.save(callbackJob);
        jobRepository.delete(this.id());
    }

    private void newPollIn(int seconds) {
        pollQueue.add(this, seconds);
    }

    @Override
    public String id() {
        return id;
    }

    public interface Id {
        JiraAddress id(String id);
    }

    public interface JiraAddress {
        IssueId to(URL address);
    }

    public interface IssueId {
        Status getStatusForIssues(List<String> issueIds);
    }

    public interface Status {
        CallbackAddress andExpectStatusEqualTo(String status);
        CallbackAddress andExpectStatusNotEqualTo(String status);
    }

    public interface CallbackAddress {
        JiraStatusJob andPostWhenReadyTo(URL callbackAddress);
    }

    public static class Builder implements Id, JiraAddress, IssueId, Status, CallbackAddress {

        private JiraStatusJob instance = new JiraStatusJob();

        public Builder(JiraClient jiraClient, PollQueue pollQueue, JobFactory jobFactory, JobRepository jobRepository) {
            requireNonNull(jiraClient);
            requireNonNull(pollQueue);
            requireNonNull(jobFactory);
            requireNonNull(jobRepository);
            instance.jiraClient = jiraClient;
            instance.pollQueue = pollQueue;
            instance.jobFactory = jobFactory;
            instance.jobRepository = jobRepository;
        }

        @Override
        public JiraAddress id(String id) {
            requireNonNull(id);
            instance.id = id;
            return this;
        }

        @Override
        public IssueId to(URL address) {
            requireNonNull(address);
            instance.address = address;
            return this;
        }

        @Override
        public Status getStatusForIssues(List<String> issueIds) {
            if (instance.issues == null)
                instance.issues = new ArrayList<>();
            instance.issues.addAll(issueIds);
            return this;
        }

        @Override
        public CallbackAddress andExpectStatusEqualTo(String status) {
            requireNonNull(status);
            instance.positiveTargetStatus = status;
            return this;
        }

        @Override
        public CallbackAddress andExpectStatusNotEqualTo(String status) {
            requireNonNull(status);
            instance.negativeTargetStatus = status;
            return this;
        }

        @Override
        public JiraStatusJob andPostWhenReadyTo(URL callbackAddress) {
            requireNonNull(callbackAddress);
            instance.callbackAddress = callbackAddress;
            return instance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JiraStatusJob that = (JiraStatusJob) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(address, that.address) &&
                Objects.equals(callbackAddress, that.callbackAddress) &&
                Objects.equals(issues, that.issues) &&
                Objects.equals(positiveTargetStatus, that.positiveTargetStatus) &&
                Objects.equals(negativeTargetStatus, that.negativeTargetStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address, callbackAddress, issues, positiveTargetStatus, negativeTargetStatus);
    }

    @Override
    public String toString() {
        return "JiraStatusJob{" +
                "id='" + id + '\'' +
                ", address=" + address +
                ", callbackAddress=" + callbackAddress +
                ", issues=" + issues +
                ", positiveTargetStatus='" + positiveTargetStatus + '\'' +
                ", negativeTargetStatus='" + negativeTargetStatus + '\'' +
                '}';
    }
}
