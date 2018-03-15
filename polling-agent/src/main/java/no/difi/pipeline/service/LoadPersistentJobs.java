package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class LoadPersistentJobs implements ApplicationListener<ApplicationStartedEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private JobRepository jobRepository;
    private PollQueue pollQueue;

    public LoadPersistentJobs(JobRepository jobRepository, PollQueue pollQueue) {
        this.jobRepository = jobRepository;
        this.pollQueue = pollQueue;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        jobRepository.load().forEach(job -> {
            pollQueue.add(job, 0);
            logger.info("Persistent job {} added to poll queue with no delay. Queue has now {} commands", job, pollQueue.size());
        });
        logger.info("Persistent jobs loaded successfully");
    }

}
