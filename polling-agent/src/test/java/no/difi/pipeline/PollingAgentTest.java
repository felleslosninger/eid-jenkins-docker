package no.difi.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
    public void givenIssueStatusIsEqualToPositiveTargetStatusWhenSendingAJiraStatusPollRequestThenJiraAndCallbackIsCalled() throws Exception {
        String anIssue = "ABC-123";
        String anIssueStatus = "5";
        CountDownLatch callbackLatch = callbackStubWithListener(anIssue);
        CountDownLatch jiraLatch = jiraRespondsWithStatusEqualTo(anIssue, anIssueStatus);
        sendToJira(aJiraStatusPollRequestForPositiveStatus(anIssue, anIssueStatus));
        assertCall(jiraLatch);
        jira.verify(getRequestedFor(urlEqualTo(jiraStatusPollURLPath(anIssue))));
        assertCall(callbackLatch);
        callback.verify(postRequestedFor(urlEqualTo(callbackURLPath(anIssue))));
    }

    @Test
    public void givenIssueStatusIsDifferentFromNegativeTargetStatusWhenSendingAJiraStatusPollRequestThenJiraAndCallbackIsCalled() throws Exception {
        String anIssue = "ABC-123";
        String anIssueStatus = "5";
        String aDifferentIssueStatus = "6";
        CountDownLatch callbackLatch = callbackStubWithListener(anIssue);
        CountDownLatch jiraLatch = jiraRespondsWithStatusEqualTo(anIssue, aDifferentIssueStatus);
        sendToJira(aJiraStatusPollRequestForNegativeStatus(anIssue, anIssueStatus));
        assertCall(jiraLatch);
        jira.verify(getRequestedFor(urlEqualTo(jiraStatusPollURLPath(anIssue))));
        assertCall(callbackLatch);
        callback.verify(postRequestedFor(urlEqualTo(callbackURLPath(anIssue))));
    }

    @Test
    public void givenIssueStatusIsEqualToTargetStatusAndCallbackListenerIsNotYetSetupWhenSendingAJiraStatusPollRequestThenCallbackIsRetriedUntilListenerIsSetUp() throws Exception {
        String anIssue = "XYZ-321";
        String anIssueStatus = "10041";
        CountDownLatch callbackLatch = callbackStubWithoutListener();
        jiraRespondsWithStatusEqualTo(anIssue, anIssueStatus);
        sendToJira(aJiraStatusPollRequestForPositiveStatus(anIssue, anIssueStatus));
        assertCall(callbackLatch);
        callbackLatch = callbackStubWithListener(anIssue);
        assertCall(callbackLatch);
        callback.verify(postRequestedFor(urlEqualTo(callbackURLPath(anIssue))));
    }

    @Test
    public void givenIssueStatusIsEqualToTargetStatusAndCallbackListenerIsNotYetSetupWhenSendingAJiraStatusPollRequestThenCallbackIsRetriedNoMoreThanFifteenSeconds() throws Exception {
        String anIssue = "XYZ-321";
        String anIssueStatus = "10041";
        CountDownLatch callbackLatch = callbackStubWithoutListener();
        jiraRespondsWithStatusEqualTo(anIssue, anIssueStatus);
        sendToJira(aJiraStatusPollRequestForPositiveStatus(anIssue, anIssueStatus));
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

    private void sendToJira(String request) throws Exception {
        mockMvc.perform(
                post("/jiraStatusPolls")
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().is(200));
    }

    private CountDownLatch jiraRespondsWithStatusEqualTo(String issue, String status) {
        jira.stubFor(
                get(jiraStatusPollURLPath(issue))
                        .willReturn(aResponse()
                                .withBody(statusResponseFromJira(status))
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

    private CountDownLatch callbackStubWithoutListener() {
        callback.stubFor(
                WireMock.post(anyUrl())
                        .willReturn(aResponse()
                                .withStatus(404))
        );
        CountDownLatch latch = new CountDownLatch(1);
        callback.addMockServiceRequestListener((request, response) -> latch.countDown());
        return latch;
    }

    private String jiraStatusPollURLPath(String issue) {
        return "/rest/api/2/issue/" + issue + "?fields=status";
    }

    private String jiraAddress() {
        return "http://localhost:" + jira.port();
    }

    private String callbackURLPath(String issue) {
        return String.format("/job/dummy-project/job/work-%s/13/input/blahblah/proceedEmpty", issue);
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

    private String statusResponseFromJira(String status) {
        return "{\"fields\":{\"status\":{\"id\":\"" + status + "\"}}}";
    }

}
