package no.difi.pipeline.service;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static jdk.incubator.http.HttpRequest.BodyProcessor.fromString;
import static jdk.incubator.http.HttpResponse.BodyHandler.asString;

public class CallbackJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ZonedDateTime created = ZonedDateTime.now();
    private URL address;
    private String onBehalfOf;
    private HttpClient httpClient;
    private PollQueue pollQueue;
    private JobRepository jobRepository;

    private CallbackJob() {
        // Use builder
    }

    public URL getAddress() {
        return address;
    }

    public String getOnBehalfOf() {
        return onBehalfOf;
    }

    public void execute() {
        try  {
            HttpResponse<String> response = httpClient.send(request(address), asString());
            if (ok(response)) {
                logger.info("Callback to {} accepted", address);
                jobRepository.delete(this.id());
            } else if (notFound(response) && SECONDS.between(created, now()) < 10L) {
                logger.info(
                        "Callback listener {} not found -- assuming it is not set up yet ({} seconds since job was registered)",
                        address,
                        SECONDS.between(created, now())
                );
                newPollIn(2);
            } else if (notFound(response)) {
                logger.info("Callback listener {} is gone", address);
                jobRepository.delete(this.id());
            } else {
                logger.warn("Callback to {} failed: HTTP status {}, retrying in a minute", address, response.statusCode());
                newPollIn(60);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Callback to {} failed, retrying in a minute. Error message: {}", address, e.getMessage());
            newPollIn(60);
        }
    }

    private void newPollIn(int seconds) {
        pollQueue.add(this, now().plus(seconds, SECONDS));
    }

    private HttpRequest request(URL url) {
        try {
            return HttpRequest.newBuilder(url.toURI()).POST(fromString("")).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI: " + url, e);
        }
    }

    private boolean ok(HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private boolean notFound(HttpResponse<?> response) {
        return response.statusCode() == 404;
    }

    @Override
    public String id() {
        return getClass().getSimpleName() + "-" + onBehalfOf;
    }

    public interface OnBehalfOf {
        CallbackAddress onBehalfOf(String job);
    }

    public interface CallbackAddress {
        CallbackJob to(URL callbackAddress);
    }

    public static class Builder implements OnBehalfOf, CallbackAddress {

        private CallbackJob instance = new CallbackJob();

        public Builder(HttpClient httpClient, PollQueue pollQueue, JobRepository jobRepository) {
            instance.httpClient = httpClient;
            instance.pollQueue = pollQueue;
            instance.jobRepository = jobRepository;
        }

        public CallbackAddress onBehalfOf(String job) {
            instance.onBehalfOf = job;
            return this;
        }

        @Override
        public CallbackJob to(URL callbackAddress) {
            instance.address = callbackAddress;
            return instance;
        }
    }
}
