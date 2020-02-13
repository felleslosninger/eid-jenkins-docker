package no.difi.pipeline.web;

import no.difi.pipeline.service.JiraStatusJob;
import no.difi.pipeline.service.JobFactory;
import no.difi.pipeline.service.PollingAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

@RestController
public class PollingAgentController {

    private PollingAgentService service;
    private JobFactory jobFactory;

    @Autowired
    public PollingAgentController(PollingAgentService service, JobFactory jobFactory) {
        this.service = service;
        this.jobFactory = jobFactory;
    }

    @PostMapping("jiraStatusPolls")
    public String addJiraStatusPoll(@Valid @RequestBody JiraStatusPoll poll) {
        return service.addJob(
                poll.positiveTargetStatus != null ?
                        jobFactory.jiraRequest()
                                .id(format("%s-%s", JiraStatusJob.class.getSimpleName(), UUID.randomUUID()))
                                .to(poll.jiraAddress)
                                .getStatusForIssues(poll.issues())
                                .andExpectStatusEqualTo(poll.positiveTargetStatus)
                                .andPostWhenReadyTo(poll.callbackAddress)
                        :
                        jobFactory.jiraRequest()
                                .id(format("%s-%s", JiraStatusJob.class.getSimpleName(), UUID.randomUUID()))
                                .to(poll.jiraAddress)
                                .getStatusForIssues(poll.issues())
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
        public String issue;
        public List<String> issues;
        @NotNull public URL callbackAddress;
        public String positiveTargetStatus;
        public String negativeTargetStatus;

        public List<String> issues() {
            if (issue != null)
                return List.of(issue);
            return issues;
        }

        @AssertTrue
        @SuppressWarnings("unused")
        private boolean isTargetStatusNotNull() {
            return (positiveTargetStatus != null && negativeTargetStatus == null) || (positiveTargetStatus == null && negativeTargetStatus != null);
        }

        @AssertTrue
        @SuppressWarnings("unused")
        private boolean isIssuesNotNull() {
            return (issues != null && !issues.isEmpty()) || issue != null;
        }

    }

}
