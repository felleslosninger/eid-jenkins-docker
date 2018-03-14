package no.difi.pipeline.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertFalse;

public class JiraClientTest {

    @Test
    public void givenJiraDoesNotRespondWhenSendingARequestThenResponsIsNotOk() throws IOException {
        JiraClient client = new JiraClient(
                null, // Will trigger NPE when sending request and consequently no response
                "aUser",
                "aPassword"
        );
        JiraClient.Response response = client.requestIssueStatus(new URL("http://jira.example.com"), "5");
        assertFalse(response.ok());
    }

}
