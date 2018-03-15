package no.difi.pipeline.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PollQueueTest {

    @Test(timeout = 1000)
    public void whenAddingCommandWithNegativeDelayThenItCanBeTakenImmediately() {
        PollQueue queue = new PollQueue();
        queue.add(aJob("5"), -100);
        assertNotNull(queue.next());
    }

    @Test(timeout = 2000)
    public void whenAddingCommandWithDelayInTheFutureThenItCanBeTakenInTheFuture() {
        PollQueue queue = new PollQueue();
        queue.add(aJob("3"), 1);
        assertNotNull(queue.next());
    }

    @Test(timeout = 1000)
    public void givenSomeCommandsWithNoDelayWhenAddingTwoCommandsWithPositiveDelayThenOldCommandsAreNextOnQueue() {
        PollQueue queue = givenSomeCommandsWithNoDelay("1", "2");
        queue.add(aJob("3"), 60);
        assertEquals("1", queue.next().id());
        queue.add(aJob("4"), 60);
        assertEquals("2", queue.next().id());
    }

    private PollQueue givenSomeCommandsWithNoDelay(String...jobIds) {
        PollQueue queue = new PollQueue();
        for (String jobId : jobIds)
            queue.add(aJob(jobId), 0);
        return queue;
    }

    private Job aJob(String id) {
        return new Job() {
            @Override
            public void execute() {

            }

            @Override
            public String id() {
                return id;
            }
        };
    }

}
