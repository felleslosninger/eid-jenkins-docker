package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Objects.requireNonNull;

public class CallbackJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ZonedDateTime firstNotFound;
    private String id;
    private URL address;
    private String onBehalfOf;
    private CallbackClient callbackClient;
    private PollQueue pollQueue;
    private JobRepository jobRepository;

    private CallbackJob() {
        // Use builder
    }

    @SuppressWarnings("unused")
    public URL getAddress() {
        return address;
    }

    @SuppressWarnings("unused")
    public String getOnBehalfOf() {
        return onBehalfOf;
    }

    public void execute() {
        CallbackClient.Response response = callbackClient.callback(address);
        if (response.ok()) {
            logger.info("Callback to {} accepted [{} ms]", address, response.requestDuration());
            jobRepository.delete(this.id());
        } else if (response.notFound()) {
            if (firstNotFound == null)
                firstNotFound = now();
            if (SECONDS.between(firstNotFound, now()) < 180L) {
                logger.info(
                        "Callback listener {} not found -- assuming it is not set up yet ({} seconds since first time not found) [{} ms]",
                        address,
                        SECONDS.between(firstNotFound, now()),
                        response.requestDuration()
                );
                newPollIn(2);
            } else {
                logger.info("Callback listener {} is gone [{} ms]", address, response.requestDuration());
                jobRepository.delete(this.id());
            }
        } else {
            logger.warn("Callback to {} failed: {} -- retrying in a minute", address, response.errorDetails());
            newPollIn(60);
        }
    }

    private void newPollIn(int seconds) {
        pollQueue.add(this, seconds);
    }

    @Override
    public String id() {
        return id;
    }

    public interface Id {
        OnBehalfOf id(String id);
    }

    public interface OnBehalfOf {
        CallbackAddress onBehalfOf(String job);
    }

    public interface CallbackAddress {
        CallbackJob to(URL callbackAddress);
    }

    public static class Builder implements Id, OnBehalfOf, CallbackAddress {

        private CallbackJob instance = new CallbackJob();

        public Builder(CallbackClient callbackClient, PollQueue pollQueue, JobRepository jobRepository) {
            instance.callbackClient = callbackClient;
            instance.pollQueue = pollQueue;
            instance.jobRepository = jobRepository;
        }

        @Override
        public OnBehalfOf id(String id) {
            requireNonNull(id);
            instance.id = id;
            return this;
        }

        @Override
        public CallbackAddress onBehalfOf(String job) {
            requireNonNull(job);
            instance.onBehalfOf = job;
            return this;
        }

        @Override
        public CallbackJob to(URL callbackAddress) {
            requireNonNull(callbackAddress);
            instance.address = callbackAddress;
            return instance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallbackJob that = (CallbackJob) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id());
    }

    @Override
    public String toString() {
        return id();
    }

}
