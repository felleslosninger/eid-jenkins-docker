package no.difi.pipeline.service;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import java.net.URISyntaxException;
import java.net.URL;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static jdk.incubator.http.HttpRequest.BodyProcessor.fromString;
import static jdk.incubator.http.HttpResponse.BodyHandler.asString;

public class CallbackClient {

    private HttpClient httpClient;

    public CallbackClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CallbackClient.Response callback(URL address) {
        HttpRequest request = request(address);
        final long t0 = currentTimeMillis();
        try {
            HttpResponse<String> httpResponse = httpClient.send(request, asString());
            return new Response(httpResponse, t0);
        } catch (Exception e) {
            return new Response(e, t0);
        }
    }

    private HttpRequest request(URL url) {
        try {
            return HttpRequest.newBuilder(url.toURI()).POST(fromString("")).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI: " + url, e);
        }
    }

    public static class Response {

        private HttpResponse<String> httpResponse;
        private Exception exception;
        private long requestDuration;

        Response(HttpResponse<String> httpResponse, long requestTime) {
            this.httpResponse = httpResponse;
            this.requestDuration = System.currentTimeMillis() - requestTime;
        }

        Response(Exception exception, long requestTime) {
            this.exception = exception;
            this.requestDuration = System.currentTimeMillis() - requestTime;
        }

        public boolean ok() {
            return httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300;
        }

        public long requestDuration() {
            return requestDuration;
        }

        public String errorDetails() {
            if (httpResponse != null) {
                return format("HTTP status %s [%d]", httpResponse.statusCode(), requestDuration);
            } else {
                return format("Exception: %s [%d]", exception.toString(), requestDuration);
            }
        }

        public boolean notFound() {
            return httpResponse.statusCode() == 404;
        }

    }
}
