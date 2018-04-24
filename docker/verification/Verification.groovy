@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5')

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*
import groovy.json.JsonSlurper

import static groovy.json.JsonOutput.toJson

import static Verification.Status.closed
import static Verification.Status.codeApproved
import static Verification.Status.codeReview
import static Verification.Status.inProgress
import static Verification.Status.manualVerification
import static Verification.Status.manualVerificationFailed
import static Verification.Status.manualVerificationOk
import static Verification.Status.open
import static Verification.Status.readyForVerification
import static Verification.Transition.approveCode
import static Verification.Transition.approveManualVerification
import static Verification.Transition.cancelVerification
import static Verification.Transition.close
import static Verification.Transition.closeWithoutStaging
import static Verification.Transition.failManualVerification
import static Verification.Transition.failStagingDeploy
import static Verification.Transition.readyForCodeReview
import static Verification.Transition.resumeWork
import static Verification.Transition.resumeWorkFromApprovedCode
import static Verification.Transition.retryManualVerificationFromFailure
import static Verification.Transition.start
import static Verification.Transition.startManualVerification
import static Verification.Transition.startVerification
import static java.util.stream.Collectors.toList

class Verification {

    enum Transition {
        start(371),
        readyForCodeReview(391),
        startVerification(291),
        cancelVerification(401),
        resumeWork(301),
        approveCode(1000001),
        resumeWorkFromApprovedCode(331),
        startManualVerification(311),
        approveManualVerification(51),
        failManualVerification(61),
        failStagingDeploy(451),
        retryManualVerificationFromSuccess(441),
        retryManualVerificationFromFailure(421),
        closeWithoutStaging(361),
        close(341)
        int id
        Transition(int id) {
            this.id = id
        }
    }

    enum Status {
        open(1),
        inProgress(3),
        readyForVerification(10717),
        codeApproved(10005),
        codeReview(10012),
        manualVerification(10009),
        manualVerificationOk(10010),
        manualVerificationFailed(10917),
        closed(1000001)
        int id
        Status(int id) {
            this.id = id
        }

        String state() {
            this == open ? "Started" : name()
        }
    }

    private String host;
    private int port;

    Verification(String host, int port) {
        this.host = host
        this.port = port
    }

    void execute() {
        normalScenario()
    }

    private void normalScenario() {
        String scenario = "normal"
        String issue = 'TEST-1234'
        newMapping(issueRequest(scenario, open.state(), issue, open))
        newMapping(issueStatusRequest(scenario, open.state(), issue, open))
        newMapping(issueTransitionRequest(scenario, open.state(), inProgress.state(), start))

        newMapping(issueRequest(scenario, inProgress.state(), issue, inProgress))
        newMapping(issueStatusRequest(scenario, inProgress.state(), issue, inProgress))
        newMapping(issueTransitionRequest(scenario, inProgress.state(), readyForVerification.state(), readyForCodeReview))

        newMapping(issueRequest(scenario, readyForVerification.state(), issue, readyForVerification))
        newMapping(issueStatusRequest(scenario, readyForVerification.state(), issue, readyForVerification))
        newMapping(issueTransitionRequest(scenario, readyForVerification.state(), codeReview.state(), startVerification))
        newMapping(issueTransitionRequest(scenario, readyForVerification.state(), inProgress.state(), cancelVerification))

        newMapping(issueRequest(scenario, codeReview.state(), issue, codeReview))
        newMapping(issueStatusRequest(scenario, codeReview.state(), issue, codeReview))
        newMapping(issueTransitionRequest(scenario, codeReview.state(), inProgress.state(), resumeWork))
        newMapping(issueTransitionRequest(scenario, codeReview.state(), codeApproved.state(), approveCode))

        newMapping(issueRequest(scenario, codeApproved.state(), issue, codeApproved))
        newMapping(issueStatusRequest(scenario, codeApproved.state(), issue, codeApproved))
        newMapping(issueTransitionRequest(scenario, codeApproved.state(), inProgress.state(), resumeWorkFromApprovedCode))
        newMapping(issueTransitionRequest(scenario, codeApproved.state(), closed.state(), closeWithoutStaging))
        newMapping(issueTransitionRequest(scenario, codeApproved.state(), manualVerificationFailed.state(), failStagingDeploy))
        newMapping(issueTransitionRequest(scenario, codeApproved.state(), manualVerification.state(), startManualVerification))
        newMapping(issuesWithStatusRequest(scenario, codeApproved.state(), manualVerification))
        newMapping(issuesWithStatusRequest(scenario, codeApproved.state(), manualVerificationOk))
        newMapping(issuesWithStatusRequest(scenario, codeApproved.state(), manualVerificationFailed))

        newMapping(issueTransitionRequest(scenario, manualVerification.state(), manualVerificationFailed.state(), failManualVerification))
        newMapping(issueTransitionRequest(scenario, manualVerification.state(), manualVerificationOk.state(), approveManualVerification))

        newMapping(issueStatusRequest(scenario, manualVerificationOk.state(), issue, manualVerificationOk))
        newMapping(issueTransitionRequest(scenario, manualVerificationOk.state(), closed.state(), close))
        // Used in Staging/Wait for approval - Jira.waitUntilManualVerificationIsFinishedAndAssertSuccess()
        newMapping(issuesWithStatusRequest(scenario, manualVerificationOk.state(), manualVerification))
        // Used in End - Jira.close() - closing self only
        newMapping(issuesWithStatusRequest(scenario, manualVerificationOk.state(), manualVerificationOk, [issue]))

        newMapping(issueTransitionRequest(scenario, manualVerificationFailed.state(), manualVerification.state(), retryManualVerificationFromFailure))

        newMapping(issueRequest(scenario, closed.state(), issue, closed))
        newMapping(issueStatusRequest(scenario, closed.state(), issue, closed))

        startBuild(issue)
        waitUntilScenarioStateIs(scenario, readyForVerification.state())
        transitionIssue(issue, startVerification)
        waitUntilScenarioStateIs(scenario, codeReview.state())
        transitionIssue(issue, approveCode)
        waitUntilScenarioStateIs(scenario, manualVerification.state())
        String buildVersion = buildVersion(issue)
        println "Current build version is ${buildVersion}"
        newMapping(issueRequest(scenario, manualVerificationOk.state(), issue, manualVerificationOk, buildVersion))
        transitionIssue(issue, approveManualVerification)
        waitForBuildToComplete(issue)
    }

    private void startBuild(String issueId) {
        post("${jobUrl(issueId)}/build?delay=0", "") // Avoid waiting the full minute until branch scanning
    }

    private void transitionIssue(String issueId, Transition transition) {
        println("Transitioning issue ${issueId} with transition ${transition}")
        post("${baseUrl()}/rest/api/2/issue/${issueId}/transitions", "{\"transition\":{\"id\":${transition.id}}}")
        println("Transition ok")
    }

    private void waitUntilScenarioStateIs(String scenarioName, String targetState) {
        println("Waiting until scenario \"${scenarioName}\" has state \"${targetState}\"")
        while (true) {
            String currentState = scenario(scenarioName).state
            if (currentState == targetState) {
                println("Scenario state is ${targetState}")
                break
            }
            println "${currentState} not equal to ${targetState}"
            sleep 1000
        }
    }

    private Map scenario(String name) {
        println("Finding scenario \"${name}\"")
        json(get("${baseUrl()}/__admin/scenarios")).scenarios.find { scenario ->
            scenario.name == name
        }
    }

    private String newMapping(String mapping) {
        println("Adding mapping")
        post("${baseUrl()}/__admin/mappings/new", mapping)
        println("Mapping added")
    }

    private String baseUrl() {
        "http://${host}:${port}"
    }

    private String waitForBuildToComplete(String issueId) {
        println "Waiting for build to complete..."
        String result = null
        while (result == null) {
            sleep 1000
            result = buildResult(issueId)
        }
        return result
    }

    private String buildResult(String issueId) {
        json(get("${buildUrl(issueId)}/api/json")).result
    }

    private String buildVersion(String issueId) {
        println("Finding build version")
        String version = json(buildDescription(issueId)).version
        println("Found build version: ${version}")
        return version
    }

    private String buildDescription(String issueId) {
        println("Finding build description")
        String description = json(get("${buildUrl(issueId)}/api/json")).description
        println("Found build description: ${description}")
        return description
    }

    private static def json(String data) {
        new JsonSlurper().parseText(data)
    }

    private static String buildUrl(String issueId) {
        "${jobUrl(issueId)}/1"
    }

    private static String jobUrl(String issueId) {
        "${jenkinsBaseUrl()}/job/verification/job/work%252F${issueId}"
    }

    private static String jenkinsBaseUrl() {
        "http://jenkins:8080"
    }

    private String get(String url) {
        HttpGet request = new HttpGet(url)
        sendRequest request
    }

    private String post(String url, String json) {
        HttpPost request = new HttpPost(url)
        request.setEntity(new StringEntity(json))
        request.addHeader('Content-Type', 'application/json')
        sendRequest request
    }

    private String sendRequest(HttpGet request) {
        println "Sending request ${request}"
        def response = HttpClientBuilder.create().build().execute(request)
        def responseBody = response.entity.content.text
        if (!response.statusLine.statusCode in (200..299))
            throw new RuntimeException("Request failed: ${response.statusLine}")
        responseBody
    }

    private String sendRequest(HttpPost request) {
        println "Sending request ${request}"
        def response = HttpClientBuilder.create().build().execute(request)
        def responseBody = response.entity.content.text
        if (!response.statusLine.statusCode in (200..299))
            throw new RuntimeException("Request failed: ${response.statusLine}")
        responseBody
    }

    // pipeline*.groovy is using this request (Jira.issueFields())
    private static String issueRequest(String scenario, String scenarioState, String issue, Status status, String version = '2018-04-18-2200') {
        """
        {
            "scenarioName": "${scenario}",
            "requiredScenarioState": "${scenarioState}",
            "request": {
                "method": "GET",
                "urlPattern": "/rest/api/2/issue/${issue}"
            },
            "response": {
                "status": 200,
                "body": "{\\"fields\\": {\\"status\\": {\\"id\\": \\"${status.id}\\"}, \\"summary\\": \\"This is a test issue\\", \\"project\\": {\\"key\\": \\"TEST\\"}, \\"fixVersions\\": [{\\"name\\": \\"${version}\\"}]}}",
                "headers": {
                  "Content-Type": "application/json"
                }
            }
        }
        """
    }

    // polling-agent is using this request
    private static String issueStatusRequest(String scenario, String scenarioState, String issue, Status status) {
        toJson(
            [
                scenarioName: scenario,
                requiredScenarioState: scenarioState,
                request: [
                    method: 'POST',
                    urlPattern: '/rest/api/2/search',
                    bodyPatterns : [
                            [ matchesJsonPath : "\$.[?(@.jql =~ /id in.*${issue}.*/)]" ]
                    ]
                ],
                response: [
                    status: 200,
                    body: "{\"issues\":[{\"key\":\"${issue}\",\"fields\": {\"status\": {\"id\": \"${status.id}\"}}}]}",
                    headers: [
                      "Content-Type": "application/json"
                    ]
                ]
            ]
        )
    }

    // pipeline*.groovy is using this request (Jira.issuesWithStatus())
    private static String issuesWithStatusRequest(String scenario, String scenarioState, Status searchStatus, List<String> issueIds = []) {
        toJson(
                [
                        scenarioName: scenario,
                        requiredScenarioState: scenarioState,
                        request: [
                                method: 'POST',
                                urlPattern: '/rest/api/2/search',
                                bodyPatterns : [
                                        [ matchesJsonPath : "\$.[?(@.jql =~ /status = ${searchStatus.id} .*/)]" ]
                                ]
                        ],
                        response: [
                                status: 200,
                                body: toJson([issues: issueIds.stream().map({toIssueObject(it)}).collect(toList())]),
                                headers: [
                                        "Content-Type": "application/json"
                                ]
                        ]
                ]
        )
    }

    private static String issueTransitionRequest(String scenario, String scenarioState, String targetScenarioState, Transition transition) {
        """
        {
            "scenarioName": "${scenario}",
            "requiredScenarioState": "${scenarioState}",
            "newScenarioState": "${targetScenarioState}",
            "request": {
                "method": "POST",
                "urlPattern": "/rest/api/2/issue/.*/transitions",
                "bodyPatterns" : [ {
                    "matchesJsonPath" : "\$.transition[?(@.id == ${transition.id})]"
                } ]
            },
            "response": {
                "status": 200
            }
        }
        """
    }

    // pipeline*.groovy is using this request (Jira.changeIssueStatus())
    private static def toIssueObject(String issueId) {
        [ key: issueId ]
    }

}