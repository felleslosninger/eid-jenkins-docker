package no.difi.pipeline.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class JobRepositoryTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private JobFactory jobFactory;
    @Mock
    private JiraClient jiraClient;
    @Mock
    private CallbackClient callbackClient;
    @Mock
    private PollQueue pollQueue;
    private JobRepository repository;

    @Before
    public void init() throws IOException {
        Path path = Files.createTempDirectory(getClass().getName());
        repository = new JobRepository(path.toFile(), jobFactory);
        when(jobFactory.jiraRequest()).thenAnswer(a -> new JiraStatusJob.Builder(jiraClient, pollQueue, jobFactory, repository));
        when(jobFactory.callbackRequest()).thenAnswer(a -> new CallbackJob.Builder(callbackClient, pollQueue, repository));
    }

    @Test
    public void givenAPersistentJiraPositiveStatusJobWhenItIsLoadedThenItIsEqualToTheJobThatWasPersisted() throws IOException {
        Job persistentJob = givenAPersistentJiraPositiveStatusJob();
        assertEquals(persistentJob, whenLoadingJob());
    }

    @Test
    public void givenAPersistentJiraNegativeStatusJobWhenItIsLoadedThenItIsEqualToTheJobThatWasPersisted() throws IOException {
        Job persistentJob = givenAPersistentJiraNegativeStatusJob();
        assertEquals(persistentJob, whenLoadingJob());
    }

    @Test
    public void givenAPersistentCallbackJobWhenItIsLoadedThenItIsEqualToTheJobThatWasPersisted() throws MalformedURLException {
        Job persistentJob = givenAPersistentCallbackJob();
        assertEquals(persistentJob, whenLoadingJob());
    }

    private Job whenLoadingJob() {
        return repository.load().get(0);
    }

    private Job givenAPersistentCallbackJob() throws MalformedURLException {
        Job job = jobFactory.callbackRequest()
                .id(format("%s-%s", CallbackJob.class.getSimpleName(), UUID.randomUUID()))
                .onBehalfOf("Whatever")
                .to(new URL("http://callback.example.com"));
        return givenAPersistentJob(job);

    }

    private Job givenAPersistentJiraPositiveStatusJob() throws MalformedURLException {
        Job job = jobFactory.jiraRequest()
                .id(format("%s-%s", JiraStatusJob.class.getSimpleName(), UUID.randomUUID()))
                .to(new URL("http://jira.example.com"))
                .getStatusForIssues(List.of("ABC-123"))
                .andExpectStatusEqualTo("13")
                .andPostWhenReadyTo(new URL("http://callback.example.com"));
        return givenAPersistentJob(job);
    }

    private Job givenAPersistentJiraNegativeStatusJob() throws IOException {
        Job job = jobFactory.jiraRequest()
                .id(format("%s-%s", JiraStatusJob.class.getSimpleName(), UUID.randomUUID()))
                .to(new URL("http://jira.example.com"))
                .getStatusForIssues(List.of("ABC-123"))
                .andExpectStatusNotEqualTo("13")
                .andPostWhenReadyTo(new URL("http://callback.example.com"));
        return givenAPersistentJob(job);
    }

    private Job givenAPersistentJob(Job job) {
        repository.save(job);
        return job;
    }

}
