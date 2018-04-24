package no.difi.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties={
        "repositoryDirectory=target/data",
        "jira.username=dummy",
        "jira.password=dummy"
})
public class PollingAgentTest {

    @Autowired
    private MockMvc mockMvc;
    @ClassRule
    public static WireMockRule jira = new WireMockRule(0);
    @ClassRule
    public static WireMockRule callback = new WireMockRule(0);
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private Environment environment;

    @After
    public void resetRules() {
        jira.resetAll();
        callback.resetAll();
        for (File file : requireNonNull(environment.getRequiredProperty("repositoryDirectory", File.class).listFiles()))
            System.out.println("File " + file + " deleted: " + file.delete());
    }

    @Test
    public void whenSendingAJiraStatusPollRequestWithoutTargetStatusThenRequestIsRejected() throws Exception {
        sendJiraStatusPollRequest(aJiraStatusPollRequestWithoutTargetStatus("ABC-123")).andExpect(status().is(400));
    }

    @Test
    public void whenSendingAJiraStatusPollRequestWithoutCallbackAddressThenRequestIsRejected() throws Exception {
        sendJiraStatusPollRequest(aJiraStatusPollRequestWithoutCallbackAddress("ABC-123", "5")).andExpect(status().is(400));
    }

    @Test
    public void givenIssueStatusIsEqualToPositiveTargetStatusWhenSendingAJiraStatusPollRequestThenJiraAndCallbackIsCalled() throws Exception {
        String anIssue = "ABC-123";
        String anIssueStatus = "5";
        CountDownLatch callbackLatch = callbackStubWithListener(anIssue);
        CountDownLatch jiraLatch = jiraRespondsWithStatusEqualTo(anIssue, anIssueStatus);
        sendJiraStatusPollRequest(aJiraStatusPollRequestForPositiveStatus(anIssue, anIssueStatus)).andExpect(status().is(200));
        assertCall(jiraLatch);
        assertCall(callbackLatch);
    }

    @Test
    public void givenIssueStatusIsDifferentFromNegativeTargetStatusWhenSendingAJiraStatusPollRequestThenJiraAndCallbackIsCalled() throws Exception {
        String anIssue = "ABC-123";
        String anIssueStatus = "5";
        String aDifferentIssueStatus = "6";
        CountDownLatch callbackLatch = callbackStubWithListener(anIssue);
        CountDownLatch jiraLatch = jiraRespondsWithStatusEqualTo(anIssue, aDifferentIssueStatus);
        sendJiraStatusPollRequest(aJiraStatusPollRequestForNegativeStatus(anIssue, anIssueStatus)).andExpect(status().is(200));
        assertCall(jiraLatch);
        assertCall(callbackLatch);
    }

    @Test
    public void givenIssueStatusIsEqualToTargetStatusAndCallbackListenerIsNotYetSetupWhenSendingAJiraStatusPollRequestThenCallbackIsRetriedUntilListenerIsSetUp() throws Exception {
        String anIssue = "XYZ-321";
        String anIssueStatus = "10041";
        CountDownLatch callbackLatch = callbackStubWithListenerAfterFirstCall();
        jiraRespondsWithStatusEqualTo(anIssue, anIssueStatus);
        sendJiraStatusPollRequest(aJiraStatusPollRequestForPositiveStatus(anIssue, anIssueStatus)).andExpect(status().is(200));
        assertCall(callbackLatch);
    }

    @Test
    @Ignore
    public void givenIssueStatusIsEqualToTargetStatusAndCallbackListenerIsNotYetSetupWhenSendingAJiraStatusPollRequestThenCallbackIsRetriedNoMoreThanFifteenSeconds() throws Exception {
        String anIssue = "XYZ-321";
        String anIssueStatus = "10041";
        CountDownLatch callbackLatch = callbackStubWithListenerAfterFirstCall();
        jiraRespondsWithStatusEqualTo(anIssue, anIssueStatus);
        sendJiraStatusPollRequest(aJiraStatusPollRequestForPositiveStatus(anIssue, anIssueStatus)).andExpect(status().is(200));
        assertCall(callbackLatch);
        Thread.sleep(15000);
        callbackLatch = callbackStubWithListener(anIssue);
        assertNoCall(callbackLatch);
    }

    private void assertCall(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(10, SECONDS));
    }

    private void assertNoCall(CountDownLatch latch) throws InterruptedException {
        assertFalse(latch.await(10, SECONDS));
    }

    private ResultActions sendJiraStatusPollRequest(String request) throws Exception {
        return mockMvc.perform(
                post("/jiraStatusPolls")
                        .contentType("application/json")
                        .content(request));
    }

    private CountDownLatch jiraRespondsWithStatusEqualTo(String issue, String status) {
        jira.stubFor(
                WireMock.post(jiraStatusPollURLPath())
                        .willReturn(aResponse()
                                .withBody(statusResponseFromJira(issue, status))
                                .withStatus(200))
        );
        CountDownLatch latch = new CountDownLatch(1);
        jira.addMockServiceRequestListener((request, response) -> latch.countDown());
        return latch;
    }

    private CountDownLatch callbackStubWithListener(String issue) {
        callback.stubFor(
                WireMock.post(callbackURLPath(issue))
                        .willReturn(aResponse()
                                .withStatus(200))
        );
        CountDownLatch latch = new CountDownLatch(1);
        callback.addMockServiceRequestListener((request, response) -> latch.countDown());
        return latch;
    }

    private CountDownLatch callbackStubWithListenerAfterFirstCall() {
        callback.stubFor(
                WireMock.post(anyUrl())
                        .inScenario("Slow setup")
                        .whenScenarioStateIs(STARTED)
                        .willSetStateTo("Set up")
                        .willReturn(aResponse()
                                .withStatus(404))
        );
        callback.stubFor(
                WireMock.post(anyUrl())
                        .inScenario("Slow setup")
                        .whenScenarioStateIs("Set up")
                        .willReturn(aResponse()
                                .withStatus(200))
        );
        CountDownLatch latch = new CountDownLatch(2);
        callback.addMockServiceRequestListener((request, response) -> latch.countDown());
        return latch;
    }


    private String jiraStatusPollURLPath() {
        return "/rest/api/2/search";
    }

    private String jiraAddress() {
        return "http://localhost:" + jira.port();
    }

    private String callbackURLPath(String issue) {
        return String.format("/job/dummy-project/job/work-%s/13/input/blahblah/proceedEmpty", issue);
    }

    private String aJiraStatusPollRequestWithoutTargetStatus(String issue) throws IOException {
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, Map.of(
                "jiraAddress", jiraAddress(),
                "callbackAddress", "http://localhost:" + callback.port() + callbackURLPath(issue),
                "issue",issue
        ));
        return stringWriter.toString();
    }

    private String aJiraStatusPollRequestWithoutCallbackAddress(String issue, String targetStatus) throws IOException {
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, Map.of(
                "jiraAddress", jiraAddress(),
                "issue",issue,
                "positiveTargetStatus", targetStatus
        ));
        return stringWriter.toString();
    }

    private String aJiraStatusPollRequestForPositiveStatus(String issue, String status) throws IOException {
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, Map.of(
                "jiraAddress", jiraAddress(),
                "callbackAddress", "http://localhost:" + callback.port() + callbackURLPath(issue),
                "issue",issue,
                "positiveTargetStatus", status
        ));
        return stringWriter.toString();
    }

    private String aJiraStatusPollRequestForNegativeStatus(String issue, String status) throws IOException {
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, Map.of(
                "jiraAddress", jiraAddress(),
                "callbackAddress", "http://localhost:" + callback.port() + callbackURLPath(issue),
                "issue", issue,
                "negativeTargetStatus", status
        ));
        return stringWriter.toString();
    }

    private String statusResponseFromJira(String issueId, String status) {
        return "{\"issues\": [{\"key\": \"" + issueId + "\", \"fields\": {\"status\": {\"id\": \"" + status + "\"}}}]}";
    }

}
