package no.difi.pipeline.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraClientTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private RestTemplate httpClient;
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
    public void givenJiraDoesNotRespondWhenSendingARequestThenResponseIsNotOk() throws IOException {
        givenJiraDoesNotRespond();
        JiraClient.Response response = whenSendingARequest();
        assertFalse(response.ok());
        assertNotNull(response.errorDetails());
    }

    @Test
    public void givenJiraRespondsWithInvalidMessageWhenSendingARequestThenResponseIsNotOk() throws IOException {
        givenJiraRespondsWithInvalidMessage();
        JiraClient.Response response = whenSendingARequest();
        assertFalse(response.ok());
        assertNotNull(response.errorDetails());
    }

    @Test
    public void whenSendingARequestThenItUsesBasicAuthentication() throws MalformedURLException {
        whenSendingARequest();
        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(httpClient).exchange(any(URI.class), any(HttpMethod.class), httpEntityCaptor.capture(), any(Class.class));
        assertTrue(httpEntityCaptor.getValue().getHeaders().get("Authorization").get(0).startsWith("Basic "));
    }

    private void givenJiraDoesNotRespond() {
        givenJiraRespondsWith(null);
    }

    private void givenJiraRespondsWithInvalidMessage() {
        ResponseEntity<String> httpResponse = new ResponseEntity<>(
                "This is an invalid response",
                HttpStatus.ACCEPTED
        );
        givenJiraRespondsWith(httpResponse);
    }

    private void givenJiraRespondsWith(ResponseEntity<String> response) {
        when(httpClient.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(response);
    }

    private JiraClient.Response whenSendingARequest() throws MalformedURLException {
        return jiraClient.requestIssueStatus(new URL("http://jira.example.com"), singletonList("ABC-123"));
    }


}
