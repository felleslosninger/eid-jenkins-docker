import groovy.json.JsonSlurper
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5')

import org.apache.http.client.methods.*
@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5')

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

import java.text.SimpleDateFormat

import static Verification.Status.*
import static Verification.Transition.*
import static groovy.json.JsonOutput.toJson
import static java.util.stream.Collectors.toList

class Verification {

    enum Transition {
        start(371),
        readyForCodeReview(391),
        startVerification(291),
        cancelVerification(401),
        resumeWork(301),
        approveCode(411),
        resumeWorkFromApprovedCode(331),
        startManualVerification(311),
        approveManualVerification(51),
        failManualVerification(431),
        failStagingDeploy(451),
        retryManualVerificationFromSuccess(441),
        retryManualVerificationFromFailure(421),
        closeWithoutTesting(461),
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
        closed(6)
        int id
        Status(int id) {
            this.id = id
        }

        String state() {
            this == open ? "Started" : name()
        }
    }

    enum IssueType {
        story(20),
        defect(11),
        techtask(13)
        int id
        IssueType(int id) {
            this.id = id
        }
    }

    enum CustomField{
        buildVersion(12480)
        int id
        CustomField(int id){
            this.id=id
        }
    }

    private String host;
    private int port;
    private String repository = "verification"
    private static File logFile = new File("/tmp/verification.log")


    Verification(String host, int port, String repository) {
        this.host = host
        this.port = port
        this.repository = repository
    }

    private static void log(String messsage){
        // if you need to log to file in verification docker container, use this.
        // Otherwise docker logs <containerId> to display println console log.
        String dateFormat = "dd.MM.yyyy;HH:mm:ss.SSS"
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat)
        String date = formatter.format(new Date())
        logFile << "${date} ${messsage}\n"
    }

    void execute(String issue) {
        //cancelWaitForCodeReviewToFinishScenario(issue)
        println "execute issue is ${issue}"
        normalScenario(issue)
//        waitForManuellVerificationScenario(issue);
//        normalScenarioTechtask(issue); // change first issuetype to tech.task in issueRequest(...)
    }

    void normalScenario(String issue) {
        String scenario = "normal"
        println "Scenario is ${scenario} and issue is ${issue}"
        mappings(scenario, issue)
        startBuild(issue)
        waitUntilJiraStatusIs(scenario, readyForVerification.state())
        transitionIssue(issue, startVerification)
        waitUntilJiraStatusIs(scenario, codeReview.state())
        transitionIssue(issue, approveCode)
        waitUntilJiraStatusIs(scenario, manualVerification.state())
        String buildVersion = buildVersion(issue)
        println "Current build version is ${buildVersion}"
        newMapping(issuesWithStatusRequest(scenario, manualVerificationOk.state(), manualVerificationOk, [issue]))  // Used in End - Jira.close() - closing self only
        newMapping(issueRequest(scenario, manualVerificationOk.state(), issue, manualVerificationOk, buildVersion))
        transitionIssue(issue, approveManualVerification)
        waitForBuildToComplete(issue)
    }

    void normalScenarioTechtask(String issue) {
        String scenario = "normal"
        println "Scenario is ${scenario} and issue is ${issue}"
        mappings(scenario, issue)
        //scenario specific mappings
        newMapping(issuesWithStatusRequest(scenario, closed.state(), manualVerificationOk))  // Used in End - Jira.close() - closing self only
        newMapping(issueRequest(scenario, closed.state(), issue, closed, buildVersion))

        startBuild(issue)
        waitUntilJiraStatusIs(scenario, readyForVerification.state())
        transitionIssue(issue, startVerification)
        waitUntilJiraStatusIs(scenario, codeReview.state())
        transitionIssue(issue, approveCode)
        String buildVersion = buildVersion(issue)
        println "Current build version is ${buildVersion}"

        waitForBuildToComplete(issue)
    }

    void waitForManuellVerificationScenario(String issue) {
        String scenario = "waitForManuellVerificationScenario"
        println "Scenario is ${scenario} and issue is ${issue}"
        mappings(scenario, issue)
        startBuild(issue)
        waitUntilJiraStatusIs(scenario, readyForVerification.state())
        transitionIssue(issue, startVerification)
        waitUntilJiraStatusIs(scenario, codeReview.state())
        transitionIssue(issue, approveCode)
        waitUntilJiraStatusIs(scenario, manualVerification.state())
        String buildVersion = buildVersion(issue)
        println "Current build version is ${buildVersion}"
        newMapping(issueRequest(scenario, manualVerificationOk.state(), issue, manualVerificationOk, buildVersion))
        newMapping(issuesWithStatusRequest(scenario, manualVerificationOk.state(), manualVerificationOk, [issue]))  // Used in End - Jira.close() - closing self only
        //transitionIssue(issue, approveManualVerification)
        waitForBuildToComplete(issue)
    }

    void cancelWaitForCodeReviewToFinishScenario(String issue) {
        String scenario = "cancelWaitForCodeReviewToFinish"
        println "Scenario is ${scenario} and issue is ${issue}"
        mappings(scenario, issue)
        startBuild(issue)
        waitUntilJiraStatusIs(scenario, readyForVerification.state())
        transitionIssue(issue, startVerification)
        waitUntilJiraStatusIs(scenario, codeReview.state())
            //waitUntilBuildStageIs("Wait for code review to finish")
        cancelBuild(issue)
        waitForBuildToComplete(issue)
    }

    private void mappings(String scenario, String issue) {
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

        newMapping(issueRequest(scenario, manualVerification.state(), issue, manualVerification))
        newMapping(issueStatusRequest(scenario, manualVerification.state(), issue, manualVerification))
        newMapping(issueTransitionRequest(scenario, manualVerification.state(), manualVerificationFailed.state(), failManualVerification))
        newMapping(issueTransitionRequest(scenario, manualVerification.state(), manualVerificationOk.state(), approveManualVerification))
        newMapping(issuesWithStatusRequest(scenario, manualVerification.state(), manualVerification, [issue]))

        newMapping(issueStatusRequest(scenario, manualVerificationOk.state(), issue, manualVerificationOk))
        newMapping(issueTransitionRequest(scenario, manualVerificationOk.state(), closed.state(), close))
        // Used in Staging/Wait for approval - Jira.waitUntilManualVerificationIsFinishedAndAssertSuccess()
        newMapping(issuesWithStatusRequest(scenario, manualVerificationOk.state(), manualVerification))

        newMapping(issueTransitionRequest(scenario, manualVerificationFailed.state(), manualVerification.state(), retryManualVerificationFromFailure))

        newMapping(issueRequest(scenario, closed.state(), issue, closed))
        newMapping(issueStatusRequest(scenario, closed.state(), issue, closed))
    }

    private void startBuild(String issueId) {
        post("${jobUrl(issueId)}/build?delay=0", "") // Avoid waiting the full minute until branch scanning
    }

    private void cancelBuild(String issueId) {
        post("${jobUrl(issueId)}/stop", "")
    }

    private void transitionIssue(String issueId, Transition transition) {
        println("Transitioning issue ${issueId} with transition ${transition}")
        post("${baseUrl()}/rest/api/2/issue/${issueId}/transitions", "{\"transition\":{\"id\":${transition.id}}}")
        println("Transition ok")
    }

    private void waitUntilJiraStatusIs(String scenarioName, String targetState) {
        println("Waiting until scenario \"${scenarioName}\" has state \"${targetState}\"")
        while (true) {
            String currentState = scenario(scenarioName).state
            if (currentState == targetState) {
                println("Scenario state is ${targetState}")
                break
            }
            println "Waiting for issue status to be ${targetState} (it is currently ${currentState})..."
            sleep 1000
        }
    }

    private Map scenario(String name) {
        json(get("${baseUrl()}/__admin/scenarios")).scenarios.find { scenario ->
            scenario.name == name
        }
    }

    private String newMapping(String mapping) {
        println("Adding mapping")
        post("${baseUrl()}/__admin/mappings/new", mapping)
        println("Mapping added")
    }

    private String repository() {
        println("Use repository ${repository}")
        "${repository}"
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

    private String buildUrl(String issueId) {
        "${jobUrl(issueId)}/1"
    }

    private String jobUrl(String issueId) {
        "${jenkinsBaseUrl()}/job/${repository()}/job/work%252F${issueId}"
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
        def response = HttpClientBuilder.create().build().execute(request)
        def responseBody = response.entity.content.text
        if (!response.statusLine.statusCode in (200..299))
            throw new RuntimeException("Request failed: ${response.statusLine}")
        responseBody
    }

    private String sendRequest(HttpPost request) {
        def response = HttpClientBuilder.create().build().execute(request)
        def responseBody = response.entity.content.text
        if (!response.statusLine.statusCode in (200..299))
            throw new RuntimeException("Request failed: ${response.statusLine}")
        responseBody
    }

    // pipeline*.groovy is using this request (Jira.issueFields())
    /**
     * See doc wiremock stubbing here: http://wiremock.org/docs/api/
     *
     * @param scenario: The name of the scenario that this stub mapping is part of
     * @param scenarioState: The required state of the scenario in order for this stub to be matched.
     * @param issue issue-id
     * @param status returned status
     * @param version default set
     * @param issueType default story
     * @return
     */
    private static String issueRequest(String scenario, String scenarioState, String issue, Status status, String version = '2018-04-18-2200', IssueType issueType = IssueType.story) {
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
                "body": "{\\"fields\\": {\\"status\\": {\\"id\\": \\"${status.id}\\"}, \\"summary\\": \\"This is a test issue\\", \\"project\\": {\\"key\\": \\"TEST\\"}, \\"issuetype\\": { \\"id\\": \\"${issueType.id}\\"}, \\"customfield_${CustomField.buildVersion.id}\\": \\"${version}\\", \\"fixVersions\\": [{\\"name\\": \\"Idporten\\"}] } }",
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