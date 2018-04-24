package no.difi.pipeline.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class JiraStatusJobTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private JiraClient jiraClient;
    @Mock
    private PollQueue pollQueue;
    @Mock
    private JobFactory jobFactory;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private CallbackClient callbackClient;

    @Before
    public void init() {
        when(jobFactory.callbackRequest()).thenReturn(new CallbackJob.Builder(callbackClient, pollQueue, jobRepository));
    }

    @Test
    public void givenIssueStatusIsDifferentThanRequestedWhenExecutingJobThenJobIsRescheduledInTenSeconds() throws Exception {
        givenIssueStatusIs(anIssue(), "3");
        Job job = jiraStatusJob(anIssue(), "7");
        job.execute();
        thenJobIsRescheduled(job, 10);
    }

    @Test
    public void givenIssueStatusIsSameAsRequestedWhenExecutingJobThenCallbackCommandIsScheduled() throws Exception {
        givenIssueStatusIs(anIssue(), "4");
        Job job = jiraStatusJob(anIssue(), "4");
        job.execute();
        thenCallbackJobIsScheduled();
    }

    @Test
    public void givenIssueStatusIsDifferentThanNegativeRequestedWhenExecutingJobThenCallbackCommandIsScheduled() throws Exception {
        givenIssueStatusIs(anIssue(), "3");
        Job job = jiraNegativeStatusJob(anIssue(), "7");
        job.execute();
        thenCallbackJobIsScheduled();
    }

    @Test
    public void givenIssueStatusIsSameAsNegativeRequestedWhenExecutingJobThenJobIsRescheduledInTenSeconds() throws Exception {
        givenIssueStatusIs(anIssue(), "8");
        Job job = jiraNegativeStatusJob(anIssue(), "8");
        job.execute();
        thenJobIsRescheduled(job, 10);
    }

    @Test
    public void givenJiraRequestFailsWhenExecutingJobThenNewCommandIsScheduledInSixtySeconds() throws MalformedURLException {
        givenJiraRequestFails();
        Job job = jiraStatusJob(anIssue(), "4");
        job.execute();
        thenJobIsRescheduled(job, 60);
    }

    private void givenIssueStatusIs(String issue, String issueStatus) {
        JiraClient.Response response = mock(JiraClient.Response.class);
        when(response.ok()).thenReturn(true);
        when(response.issueStatuses()).thenReturn(Map.of(issue, issueStatus));
        givenJiraResponse(response);
    }

    private void givenJiraRequestFails() {
        JiraClient.Response response = mock(JiraClient.Response.class);
        when(response.ok()).thenReturn(false);
        when(response.issueStatuses()).thenReturn(null);
        when(response.errorDetails()).thenReturn("Some failure");
        givenJiraResponse(response);
    }

    private void givenJiraResponse(JiraClient.Response response) {
        when(jiraClient.requestIssueStatus(any(URL.class), anyList())).thenReturn(response);
    }

    private void thenJobIsRescheduled(Job job, int delaySeconds) {
        verify(pollQueue).add(eq(job), eq(delaySeconds));
    }

    private void thenCallbackJobIsScheduled() {
        verify(pollQueue).add(any(CallbackJob.class), eq(0));
    }

    private JiraStatusJob jiraStatusJob(String issue, String issueStatus) throws MalformedURLException {
        return jobBuilder()
                .to(new URL("http://jira.example.com"))
                .getStatusForIssues(List.of(issue))
                .andExpectStatusEqualTo(issueStatus)
                .andPostWhenReadyTo(new URL("http://callback.example.com"));
    }

    private JiraStatusJob jiraNegativeStatusJob(String issue, String issueStatus) throws MalformedURLException {
        return jobBuilder()
                .to(new URL("http://jira.example.com"))
                .getStatusForIssues(List.of(issue))
                .andExpectStatusNotEqualTo(issueStatus)
                .andPostWhenReadyTo(new URL("http://callback.example.com"));
    }

    private JiraStatusJob.JiraAddress jobBuilder() {
        return new JiraStatusJob.Builder(jiraClient, pollQueue, jobFactory, jobRepository).id(UUID.randomUUID().toString());
    }

    private String anIssue() {
        return "ABC-123";
    }

}
