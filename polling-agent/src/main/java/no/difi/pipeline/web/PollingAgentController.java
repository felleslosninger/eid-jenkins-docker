package no.difi.pipeline.web;

import no.difi.pipeline.service.JobFactory;
import no.difi.pipeline.service.PollingAgentService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URL;

@RestController
public class PollingAgentController {

    private PollingAgentService service;
    private JobFactory jobFactory;

    public PollingAgentController(PollingAgentService service, JobFactory jobFactory) {
        this.service = service;
        this.jobFactory = jobFactory;
    }

    @PostMapping("jiraStatusPolls")
    public String addJiraStatusPoll(@Valid @RequestBody JiraStatusPoll poll) {
        return service.addJob(
                poll.positiveTargetStatus != null ?
                        jobFactory.jiraRequest()
                                .to(poll.jiraAddress)
                                .getStatusForIssue(poll.issue)
                                .andExpectStatusEqualTo(poll.positiveTargetStatus)
                                .andPostWhenReadyTo(poll.callbackAddress)
                        :
                        jobFactory.jiraRequest()
                                .to(poll.jiraAddress)
                                .getStatusForIssue(poll.issue)
                                .andExpectStatusNotEqualTo(poll.negativeTargetStatus)
                                .andPostWhenReadyTo(poll.callbackAddress)
        );
    }

    @DeleteMapping("jiraStatusPolls/{id}")
    public void deleteJiraStatusPoll(@PathVariable String id) {
        service.deleteJob(id);
    }

    public static class JiraStatusPoll {
        @NotNull URL jiraAddress;
        @NotNull String issue;
        String positiveTargetStatus;
        String negativeTargetStatus;
        @NotNull URL callbackAddress;

        public URL getJiraAddress() {
            return jiraAddress;
        }

        public void setJiraAddress(URL jiraAddress) {
            this.jiraAddress = jiraAddress;
        }

        public String getIssue() {
            return issue;
        }

        public void setIssue(String issue) {
            this.issue = issue;
        }

        public String getPositiveTargetStatus() {
            return positiveTargetStatus;
        }

        public void setPositiveTargetStatus(String positiveTargetStatus) {
            this.positiveTargetStatus = positiveTargetStatus;
        }

        public String getNegativeTargetStatus() {
            return negativeTargetStatus;
        }

        public void setNegativeTargetStatus(String negativeTargetStatus) {
            this.negativeTargetStatus = negativeTargetStatus;
        }

        public URL getCallbackAddress() {
            return callbackAddress;
        }

        public void setCallbackAddress(URL callbackAddress) {
            this.callbackAddress = callbackAddress;
        }
    }

}
