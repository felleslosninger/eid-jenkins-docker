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

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.springframework.http.HttpMethod.GET;

public class JiraClient {

    private RestTemplate httpClient;
    private String username;
    private String password;

    public JiraClient(RestTemplate httpClient, String username, String password) {
        this.httpClient = httpClient;
        this.username = username;
        this.password = password;
    }

    public Response requestIssueStatus(URL address, String issue) {
        final long t0 = currentTimeMillis();
        try {
            ResponseEntity<String> httpResponse = httpClient.exchange(
                    requestUri(address, issue),
                    GET,
                    new HttpEntity(requestHeaders()),
                    String.class
            );
            return new Response(httpResponse, t0);
        } catch (Exception e) {
            return new Response(e, t0);
        }
    }

    private URI requestUri(URL address, String issue) {
        return URI.create(format("%s/rest/api/2/issue/%s?fields=status", address, issue));
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
        private String issueStatus;
        private long requestDuration;

        Response(ResponseEntity<String> httpResponse, long requestTime) {
            this.httpResponse = httpResponse;
            this.requestDuration = System.currentTimeMillis() - requestTime;
            try {
                issueStatus = parseIssueStatus();
            } catch (RuntimeException e) {
                this.exception = e;
            }
        }

        Response(Exception exception, long requestTime) {
            this.exception = exception;
            this.requestDuration = System.currentTimeMillis() - requestTime;
        }

        public boolean ok() {
            return httpResponse != null && httpResponse.getStatusCode().is2xxSuccessful() && issueStatus != null;
        }

        public String issueStatus() {
            return issueStatus;
        }

        private String parseIssueStatus() {
            if (!httpResponse.hasBody())
                throw new RuntimeException("Jira response is empty");
            JsonValue jsonResponse = Json.createReader(new StringReader(httpResponse.getBody())).readValue();
            if (jsonResponse.getValueType() != JsonValue.ValueType.OBJECT)
                throw new RuntimeException("Jira response contains no Json object");
            return jsonResponse.asJsonObject().getJsonObject("fields").getJsonObject("status").getString("id");
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
