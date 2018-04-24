package no.difi.pipeline.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpMethod.POST;

public class JiraClient {

    private RestTemplate httpClient;
    private String username;
    private String password;

    public JiraClient(RestTemplate httpClient, String username, String password) {
        this.httpClient = httpClient;
        this.username = username;
        this.password = password;
    }

    public Response requestIssueStatus(URL address, List<String> issues) {
        final long t0 = currentTimeMillis();
        try {
            ResponseEntity<String> httpResponse = httpClient.exchange(
                    requestUri(address),
                    POST,
                    new HttpEntity<>(requestBody(issues), requestHeaders()),
                    String.class
            );
            return new Response(httpResponse, t0);
        } catch (Exception e) {
            return new Response(e, t0);
        }
    }

    private String requestBody(List<String> issues) {
        return format("{\"jql\": \"id in (%s)\", \"fields\": [ \"status\" ]}", join(",", issues));
    }

    private URI requestUri(URL address) {
        return URI.create(format("%s/rest/api/2/search", address));
    }

    private HttpHeaders requestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth());
        return headers;
    }

    private String auth() {
        try {
            return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Response {

        private ResponseEntity<String> httpResponse;
        private Exception exception;
        private Map<String, String> issueStatuses;
        private long requestDuration;

        Response(ResponseEntity<String> httpResponse, long requestTime) {
            this.httpResponse = httpResponse;
            this.requestDuration = System.currentTimeMillis() - requestTime;
            try {
                issueStatuses = parseIssueStatuses();
            } catch (RuntimeException e) {
                this.exception = e;
            }
        }

        Response(Exception exception, long requestTime) {
            this.exception = exception;
            this.requestDuration = System.currentTimeMillis() - requestTime;
        }

        public boolean ok() {
            return httpResponse != null && httpResponse.getStatusCode().is2xxSuccessful() && issueStatuses != null;
        }

        public Map<String, String> issueStatuses() {
            return issueStatuses;
        }

        private Map<String, String> parseIssueStatuses() {
            if (!httpResponse.hasBody())
                throw new RuntimeException("Jira response is empty");
            JsonValue jsonResponse = Json.createReader(new StringReader(httpResponse.getBody())).readValue();
            if (jsonResponse.getValueType() != JsonValue.ValueType.OBJECT)
                throw new RuntimeException("Jira response contains no Json object");
            if (jsonResponse.asJsonObject().getJsonArray("issues") == null)
                throw new RuntimeException("Jira response contains no Json object with issues array");
            return jsonResponse.asJsonObject()
                    .getJsonArray("issues")
                    .stream()
                    .map(JsonValue::asJsonObject)
                    .collect(toMap(
                            issue -> issue.getString("key"),
                            issue -> issue.getJsonObject("fields").getJsonObject("status").getString("id")
                            )
                    );
        }

        public long requestDuration() {
            return requestDuration;
        }

        public String errorDetails() {
            if (exception != null) {
                return format("Exception: %s [%d]", exception.toString(), requestDuration);
            } else if (httpResponse != null) {
                return format("HTTP status %s [%d]", httpResponse.getStatusCodeValue(), requestDuration);
            } else {
                return null;
            }
        }
    }
}
