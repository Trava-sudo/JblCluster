package hub.ebb.jblcluster.eventservice.service;

import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.ebb.jblcluster.eventservice.generator.InvalidTypeException;
import hub.ebb.jblcluster.eventservice.service.impl.MainEventFactoryImpl;

public interface MainEventFactory {

    <T extends JpsEvent> T buildEvent(final String eventSpecCode, final String eventType) throws InvalidTypeException, hub.jbl.core.generator.InvalidTypeException;

    <T extends JpsEvent> T initEvent(T event) throws InvalidTypeException, hub.jbl.core.generator.InvalidTypeException;

    static MainEventFactory getInstance(){ return new MainEventFactoryImpl();};

}
