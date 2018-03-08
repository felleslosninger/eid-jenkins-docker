package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.time.ZonedDateTime.now;
import static java.util.stream.Collectors.toList;

@Component
public class PollQueue {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DelayQueue<PollCommand> queue = new DelayQueue<>();

    public void add(Job job, ZonedDateTime executeTime) {
        List<PollCommand> oldCommands = queue.stream().filter(e -> e.job.id().equals(job.id())).collect(toList());
        if (!oldCommands.isEmpty()) {
            logger.info("Removing {} obsolete command(s) from poll queue", oldCommands.size());
        }
        queue.add(new PollCommand(job, executeTime));
        queue.removeAll(oldCommands);
    }

    public void remove(String jobId) {
        queue.removeIf(c -> c.job.id().equals(jobId));
    }

    public void executeNext() {
        try {
            queue.take().execute();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to take on queue", e);
        }
    }

    private class PollCommand implements Delayed {

        private Job job;
        private ZonedDateTime executeTime;

        PollCommand(Job job, ZonedDateTime executeTime) {
            this.job = job;
            this.executeTime = executeTime;
        }

        void execute() {
            job.execute();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.toChronoUnit().between(now(), executeTime);
        }

        @Override
        public int compareTo(Delayed o) {
            long delay = getDelay(TimeUnit.NANOSECONDS);
            long otherDelay = o.getDelay(TimeUnit.NANOSECONDS);
            if (delay < otherDelay) return -1;
            if (otherDelay > delay) return 1;
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PollCommand that = (PollCommand) o;
            return Objects.equals(job, that.job) &&
                    Objects.equals(executeTime, that.executeTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(job, executeTime);
        }
    }

}
