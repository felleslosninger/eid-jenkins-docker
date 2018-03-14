package no.difi.pipeline.service;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraClientTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private HttpClient httpClient;
    private JiraClient jiraClient;

    @Before
    public void init() {
        jiraClient = new JiraClient(
                httpClient,
                "aUser",
                "aPassword"
        );
    }

    @Test
    public void givenJiraDoesNotRespondWhenSendingARequestThenResponseIsNotOk() throws IOException, InterruptedException {
        givenJiraDoesNotRespond();
        JiraClient.Response response = jiraClient.requestIssueStatus(new URL("http://jira.example.com"), "5");
        assertFalse(response.ok());
        assertNotNull(response.errorDetails());
    }

    @Test
    public void givenJiraRespondsWithInvalidMessageWhenSendingARequestThenResponseIsNotOk() throws IOException, InterruptedException {
        givenJiraRespondsWithInvalidMessage();
        JiraClient.Response response = jiraClient.requestIssueStatus(new URL("http://jira.example.com"), "5");
        assertFalse(response.ok());
        assertNotNull(response.errorDetails());
    }

    private void givenJiraDoesNotRespond() throws IOException, InterruptedException {
        givenJiraRespondsWith(null);
    }

    private void givenJiraRespondsWithInvalidMessage() throws IOException, InterruptedException {
        @SuppressWarnings("unchecked")
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("This is an invalid response");
        givenJiraRespondsWith(httpResponse);
    }

    @SuppressWarnings("unchecked")
    private void givenJiraRespondsWith(HttpResponse<String> response) throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

}
