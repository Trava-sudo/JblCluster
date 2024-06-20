package hub.ebb.jblcluster.eventservice.service;

import java.util.Random;

public class EventSequenceNumberGenerator {

    private static final EventSequenceNumberGenerator INSTANCE = new EventSequenceNumberGenerator();
    private Random random = new Random();

    private EventSequenceNumberGenerator() {
    }

    public static EventSequenceNumberGenerator getInstance() {
        return INSTANCE;
    }

    synchronized public int nextInt() {
        return random.nextInt();
    }
}
