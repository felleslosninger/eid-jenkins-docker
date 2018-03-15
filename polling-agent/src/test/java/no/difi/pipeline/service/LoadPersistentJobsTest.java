package no.difi.pipeline.service;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class LoadPersistentJobsTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private JiraClient jiraClient;
    @Mock
    private JobFactory jobFactory;
    @Mock
    private JobRepository jobRepository;
    private PollQueue pollQueue = new PollQueue();

    @Test(timeout = 1000)
    public void test() {
        givenJobRepositoryHasJobs(5);
        whenLoadingJobs();
        assertEquals(5, pollQueue.size());
        for (int i = 0; i < 5; i++)
            pollQueue.next();
    }

    private void givenJobRepositoryHasJobs(int numberOfJobs) {
        when(jobRepository.load()).thenReturn(
                IntStream.range(0, numberOfJobs).mapToObj(n -> aJob(n)).collect(toList())
        );
    }

    private void whenLoadingJobs() {
        LoadPersistentJobs command = new LoadPersistentJobs(jobRepository, pollQueue);
        command.onApplicationEvent(null);
    }

    private Job aJob(int i) {
        try {
            return jobBuilder()
                    .to(new URL("http://jira.example.com"))
                    .getStatusForIssue("ABC-" + i)
                    .andExpectStatusEqualTo("3")
                    .andPostWhenReadyTo(new URL("http://callback.example.com"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private JiraStatusJob.Builder jobBuilder() {
        return new JiraStatusJob.Builder(jiraClient, pollQueue, jobFactory, jobRepository);
    }

}
