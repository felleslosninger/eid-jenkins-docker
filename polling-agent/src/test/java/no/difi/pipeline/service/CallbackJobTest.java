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

public class CallbackJobTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private PollQueue pollQueue;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private CallbackClient callbackClient;

    @Before
    public void init() {
    }

    @Test
    public void givenCallbackSucceedsThenNoJobIsScheduled() throws MalformedURLException {
        givenCallbackRequestSucceeds();
        callbackJob().execute();
        thenNoJobIsScheduled();
    }

    @Test
    public void givenCallbackFailsThenJobIsRescheduledInSixtySeconds() throws MalformedURLException {
        givenCallbackRequestFails();
        Job job = callbackJob();
        job.execute();
        thenJobIsRescheduled(job, 60);
    }

    @Test
    public void givenCallbackListenerIsNotFoundThenJobIsRescheduledInTwoSeconds() throws MalformedURLException {
        givenCallbackListenerIsNotFound();
        Job job = callbackJob();
        job.execute();
        thenJobIsRescheduled(job, 2);
    }

    private void givenCallbackRequestSucceeds() {
        CallbackClient.Response response = mock(CallbackClient.Response.class);
        when(response.ok()).thenReturn(true);
        givenCallbackResponse(response);
    }

    private void givenCallbackRequestFails() {
        CallbackClient.Response response = mock(CallbackClient.Response.class);
        when(response.ok()).thenReturn(false);
        when(response.notFound()).thenReturn(false);
        when(response.errorDetails()).thenReturn("Some failure");
        givenCallbackResponse(response);
    }

    private void givenCallbackListenerIsNotFound() {
        CallbackClient.Response response = mock(CallbackClient.Response.class);
        when(response.ok()).thenReturn(false);
        when(response.notFound()).thenReturn(true);
        when(response.errorDetails()).thenReturn("Some failure");
        givenCallbackResponse(response);
    }

    private void thenJobIsRescheduled(Job job, int delaySeconds) {
        verify(pollQueue).add(eq(job), eq(delaySeconds));
    }

    private void thenNoJobIsScheduled() {
        verifyZeroInteractions(pollQueue);
    }

    private void givenCallbackResponse(CallbackClient.Response response) {
        when(callbackClient.callback(any(URL.class))).thenReturn(response);
    }

    private CallbackJob callbackJob() throws MalformedURLException {
        return jobBuilder().to(new URL("http://callback.example.com"));
    }

    private CallbackJob.Builder jobBuilder() {
        return new CallbackJob.Builder(callbackClient, pollQueue, jobRepository);
    }

}
