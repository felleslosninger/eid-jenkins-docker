package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static java.time.ZonedDateTime.now;

@Service
public class PollingAgentService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PollQueue pollQueue;
    private final JobRepository jobRepository;

    public PollingAgentService(PollQueue pollQueue, JobRepository jobRepository) {
        this.pollQueue = pollQueue;
        this.jobRepository = jobRepository;
        jobRepository.load().forEach(j -> pollQueue.add(j, now()));
        new PollingWorker().start();
    }

    public String addJob(Job job) {
        logger.info("Adding job " + job.id());
        jobRepository.save(job);
        pollQueue.add(job, now());
        return job.id();
    }

    public void deleteJob(String id) {
        logger.info("Deleting job " + id);
        jobRepository.delete(id);
        pollQueue.remove(id);
    }

    private class PollingWorker extends Thread {

        PollingWorker() {
            super("PollingWorker");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                pollQueue.executeNext();
            }
        }

    }

}
