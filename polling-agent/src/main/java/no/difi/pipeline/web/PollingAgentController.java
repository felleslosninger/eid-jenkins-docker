package no.difi.pipeline.web;

import no.difi.pipeline.service.JobFactory;
import no.difi.pipeline.service.PollingAgentService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
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

    @SuppressWarnings("WeakerAccess")
    public static class JiraStatusPoll {

        @NotNull public URL jiraAddress;
        @NotNull public String issue;
        @NotNull public URL callbackAddress;
        public String positiveTargetStatus;
        public String negativeTargetStatus;

        @AssertTrue
        @SuppressWarnings("unused")
        private boolean isTargetStatusNotNull() {
            return (positiveTargetStatus != null && negativeTargetStatus == null) || (positiveTargetStatus == null && negativeTargetStatus != null);
        }

    }

}
