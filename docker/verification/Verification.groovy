@Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.5')

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*
import groovy.json.JsonSlurper

class Verification {

    private String host;
    private int port;

    Verification(String host, int port) {
        this.host = host
        this.port = port
    }

    void execute() {
        givenNormalScenario()
        get("${baseUrl()}/job/verification/build?delay=0") // Avoid waiting the full minute until branch scanning
        waitUntilScenarioStateIs('Pipeline','Ready for code review')
        def process = 'docker service scale pipeline_jenkins=0'.execute()
        process.waitForProcessOutput(System.out, System.err)
        process.waitForOrKill(1_000_000)
        transitionIssue('TEST-1234', 291)
        process = 'docker service scale pipeline_jenkins=1'.execute()
        process.waitForProcessOutput(System.out, System.err)
        process.waitForOrKill(1_000_000)
    }

    private void transitionIssue(String issueId, int transitionId) {
        post("${baseUrl()}/rest/api/2/issue/TEST-1234/transitions", "{\"transition\":{\"id\":${transitionId}}}")
    }

    private void waitUntilScenarioStateIs(String scenarioName, String targetState) {
        while (true) {
            String currentState = scenario(scenarioName).state
            if (currentState == targetState) break
            println "${currentState} not equal to ${targetState}"
            sleep 1000
        }
    }

    private Map scenario(String name) {
        new JsonSlurper().parseText(get("${baseUrl()}/__admin/scenarios")).scenarios.find { scenario ->
            scenario.name == name
        }
    }

    private void givenNormalScenario() {
        newMapping(issueStatusRequest("Started", 3))
        newMapping(issueStatusRequest("Code review", 10012))
        newMapping(issueTransitionRequest("Started", "Ready for code review", 391))
        newMapping(issueTransitionRequest("Code review", "In progress", 301))
        newMapping(issueTransitionRequest("Ready for code review", "Code review", 291))
    }

    private String newMapping(String mapping) {
        post("${baseUrl()}/__admin/mappings/new", mapping)
    }

    private String baseUrl() {
        "http://${host}:${port}"
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
        HttpClientBuilder.create().build().execute(request).entity.content.text
    }

    private String sendRequest(HttpPost request) {
        HttpClientBuilder.create().build().execute(request)
    }

    private static String issueStatusRequest(String scenarioState, int status) {
        """
        {
            "scenarioName": "Pipeline",
            "requiredScenarioState": "${scenarioState}",
            "request": {
                "method": "GET",
                "urlPattern": "/rest/api/2/issue/.*"
            },
            "response": {
                "status": 200,
                "body": "{\\"fields\\": {\\"status\\": {\\"id\\": \\"${status}\\"}}}",
                "headers": {
                  "Content-Type": "application/json"
                }
            }
        }
        """
    }

    private static String issueTransitionRequest(String scenarioState, String targetScenarioState, int transition) {
        """
        {
            "scenarioName": "Pipeline",
            "requiredScenarioState": "${scenarioState}",
            "newScenarioState": "${targetScenarioState}",
            "request": {
                "method": "POST",
                "urlPattern": "/rest/api/2/issue/.*/transitions",
                "bodyPatterns" : [ {
                    "matchesJsonPath" : "\$.transition[?(@.id == ${transition})]"
                } ]
            },
            "response": {
                "status": 200
            }
        }
        """
    }

}