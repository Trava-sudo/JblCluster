package hub.ebb.jblcluster.eventservice.generator;

/**
 * Created by Petar Tseperski on 02/08/2017.
 */
public class InvalidTypeException extends Exception {
    public InvalidTypeException(String message) {
        super( message );
    }

    public InvalidTypeException(String message, String eventType) {
        super( "Event type: " + eventType + "MESSAGE: " + message );
    }
}
