package no.difi.pipeline.service;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static jdk.incubator.http.HttpResponse.BodyHandler.asString;

public class JiraClient {

    private HttpClient httpClient;
    private String username;
    private String password;

    public JiraClient(HttpClient httpClient, String username, String password) {
        this.httpClient = httpClient;
        this.username = username;
        this.password = password;
    }

    public Response requestIssueStatus(URL address, String issue) {
        HttpRequest request = request(address, issue);
        final long t0 = currentTimeMillis();
        try {
            HttpResponse<String> httpResponse = httpClient.send(request, asString());
            return new Response(httpResponse, t0);
        } catch (Exception e) {
            return new Response(e, t0);
        }
    }

    private HttpRequest request(URL address, String issue) {
        URI uri = URI.create(format("%s/rest/api/2/issue/%s?fields=status", address, issue));
        return HttpRequest.newBuilder(uri).GET()
                .setHeader("Authorization", "Basic " + auth())
                .build();
    }

    private String auth() {
        try {
            return Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Response {

        private HttpResponse<String> httpResponse;
        private Exception exception;
        private String issueStatus;
        private long requestDuration;

        Response(HttpResponse<String> httpResponse, long requestTime) {
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
            return httpResponse != null && httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300 && issueStatus != null;
        }

        public String issueStatus() {
            return issueStatus;
        }

        private String parseIssueStatus() {
            if (httpResponse.body().isEmpty())
                throw new RuntimeException("Jira response is empty");
            JsonValue jsonResponse = Json.createReader(new StringReader(httpResponse.body())).readValue();
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
                return format("HTTP status %s [%d]", httpResponse.statusCode(), requestDuration);
            } else {
                return null;
            }
        }
    }
}
