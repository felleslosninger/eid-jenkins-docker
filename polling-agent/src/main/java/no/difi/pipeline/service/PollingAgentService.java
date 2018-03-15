package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PollingAgentService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PollQueue pollQueue;
    private final JobRepository jobRepository;

    public PollingAgentService(PollQueue pollQueue, JobRepository jobRepository) {
        this.pollQueue = pollQueue;
        this.jobRepository = jobRepository;
    }

    public String addJob(Job job) {
        logger.info("Adding job " + job.id());
        jobRepository.save(job);
        pollQueue.add(job, 0);
        return job.id();
    }

    public void deleteJob(String id) {
        logger.info("Deleting job " + id);
        jobRepository.delete(id);
        pollQueue.remove(id);
    }

}
