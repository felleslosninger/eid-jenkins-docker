package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class JiraStatusJob implements Job {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private URL address;
    private URL callbackAddress;
    private String issue;
    private String positiveTargetStatus;
    private String negativeTargetStatus;
    private JiraClient jiraClient;
    private PollQueue pollQueue;
    private JobFactory jobFactory;
    private JobRepository jobRepository;

    @SuppressWarnings("unused")
    public URL getAddress() {
        return address;
    }

    @SuppressWarnings("unused")
    public URL getCallbackAddress() {
        return callbackAddress;
    }

    @SuppressWarnings("unused")
    public String getIssue() {
        return issue;
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
        JiraClient.Response response = jiraClient.requestIssueStatus(address, issue);
        if (!response.ok()) {
            logger.warn("Requesting status for issue {} failed: {}", issue, response.errorDetails());
            newPollIn(60);
        } else if (positiveTargetStatus != null) {
            String issueStatus = response.issueStatus();
            if (!positiveTargetStatus.equals(issueStatus)) {
                logger.info("Status for issue {} is not yet {} (it is {}) [{} ms]", issue, positiveTargetStatus, issueStatus, response.requestDuration());
                newPollIn(10);
            } else {
                logger.info("Success - status for issue {} is now {} [{} ms]", issue, issueStatus, response.requestDuration());
                newCallbackJob();
            }
        } else if (negativeTargetStatus != null) {
            String issueStatus = response.issueStatus();
            if (negativeTargetStatus.equals(issueStatus)) {
                logger.info("Status for issue {} is still {}) [{} ms]", issue, negativeTargetStatus, response.requestDuration());
                newPollIn(10);
            } else {
                logger.info("Success - status for issue {} is now not {} (it is {}) [{} ms]", issue, negativeTargetStatus, issueStatus, response.requestDuration());
                newCallbackJob();
            }
        } else {
            throw new IllegalStateException(format("No target status defined for issue %s", issue));
        }
    }

    private void newCallbackJob() {
        Job callbackJob = jobFactory.callbackRequest().onBehalfOf(id()).to(callbackAddress);
        pollQueue.add(callbackJob, 0);
        jobRepository.save(callbackJob);
        jobRepository.delete(this.id());
    }

    private void newPollIn(int seconds) {
        pollQueue.add(this, seconds);
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
        public IssueId to(URL address) {
            requireNonNull(address);
            instance.address = address;
            return this;
        }

        @Override
        public Status getStatusForIssue(String issueId) {
            requireNonNull(issueId);
            instance.issue = issueId;
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
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id());
    }
}
