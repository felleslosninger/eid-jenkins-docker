package no.difi.pipeline.service;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PollQueueTest {

    @Test(timeout = 1000)
    public void whenAddingCommandWithNegativeDelayThenItCanBeTakenImmediately() {
        PollQueue queue = new PollQueue();
        queue.add(dummyJob(), -100);
        assertNotNull(queue.next());
    }

    @Test(timeout = 2000)
    public void whenAddingCommandWithDelayInTheFutureThenItCanBeTakenInTheFuture() {
        PollQueue queue = new PollQueue();
        queue.add(dummyJob(), 1);
        assertNotNull(queue.next());
    }

    private Job dummyJob() {
        return new Job() {
            @Override
            public void execute() {

            }

            @Override
            public String id() {
                return null;
            }
        };
    }

}
