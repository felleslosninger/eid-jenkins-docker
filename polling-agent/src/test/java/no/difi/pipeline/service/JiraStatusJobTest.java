package no.difi.pipeline.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.MalformedURLException;
import java.net.URL;

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
        givenIssueStatusIs("3");
        Job job = jiraStatusJob("7");
        job.execute();
        thenJobIsRescheduled(job, 10);
    }

    @Test
    public void givenIssueStatusIsSameAsRequestedWhenExecutingJobThenCallbackCommandIsScheduled() throws Exception {
        givenIssueStatusIs("4");
        Job job = jiraStatusJob("4");
        job.execute();
        thenCallbackJobIsScheduled();
    }

    @Test
    public void givenIssueStatusIsDifferentThanNegativeRequestedWhenExecutingJobThenCallbackCommandIsScheduled() throws Exception {
        givenIssueStatusIs("3");
        Job job = jiraNegativeStatusJob("7");
        job.execute();
        thenCallbackJobIsScheduled();
    }

    @Test
    public void givenIssueStatusIsSameAsNegativeRequestedWhenExecutingJobThenJobIsRescheduledInTenSeconds() throws Exception {
        givenIssueStatusIs("8");
        Job job = jiraNegativeStatusJob("8");
        job.execute();
        thenJobIsRescheduled(job, 10);
    }

    @Test
    public void givenJiraRequestFailsWhenExecutingJobThenNewCommandIsScheduledInSixtySeconds() throws MalformedURLException {
        givenJiraRequestFails();
        Job job = jiraStatusJob("4");
        job.execute();
        thenJobIsRescheduled(job, 60);
    }

    private void givenIssueStatusIs(String issueStatus) {
        JiraClient.Response response = mock(JiraClient.Response.class);
        when(response.ok()).thenReturn(true);
        when(response.issueStatus()).thenReturn(issueStatus);
        givenJiraResponse(response);
    }

    private void givenJiraRequestFails() {
        JiraClient.Response response = mock(JiraClient.Response.class);
        when(response.ok()).thenReturn(false);
        when(response.issueStatus()).thenReturn(null);
        when(response.errorDetails()).thenReturn("Some failure");
        givenJiraResponse(response);
    }

    private void givenJiraResponse(JiraClient.Response response) {
        when(jiraClient.requestIssueStatus(any(URL.class), any(String.class))).thenReturn(response);
    }

    private void thenJobIsRescheduled(Job job, int delaySeconds) {
        verify(pollQueue).add(eq(job), eq(delaySeconds));
    }

    private void thenCallbackJobIsScheduled() {
        verify(pollQueue).add(any(CallbackJob.class), eq(0));
    }

    private JiraStatusJob jiraStatusJob(String issueStatus) throws MalformedURLException {
        return jobBuilder()
                .to(new URL("http://jira.example.com"))
                .getStatusForIssue("ABC-123")
                .andExpectStatusEqualTo(issueStatus)
                .andPostWhenReadyTo(new URL("http://callback.example.com"));
    }

    private JiraStatusJob jiraNegativeStatusJob(String issueStatus) throws MalformedURLException {
        return jobBuilder()
                .to(new URL("http://jira.example.com"))
                .getStatusForIssue("ABC-123")
                .andExpectStatusNotEqualTo(issueStatus)
                .andPostWhenReadyTo(new URL("http://callback.example.com"));
    }

    private JiraStatusJob.Builder jobBuilder() {
        return new JiraStatusJob.Builder(jiraClient, pollQueue, jobFactory, jobRepository);
    }

}
