package no.difi.pipeline.service;

import org.springframework.stereotype.Component;

@Component
public class PollQueueWorker extends Thread {

    private final PollQueue pollQueue;

    public PollQueueWorker(PollQueue pollQueue) {
        super("PollingWorker");
        setDaemon(true);
        this.pollQueue = pollQueue;
        start();
    }

    @Override
    public void run() {
        while (true) {
            pollQueue.next().execute();
        }
    }

}
