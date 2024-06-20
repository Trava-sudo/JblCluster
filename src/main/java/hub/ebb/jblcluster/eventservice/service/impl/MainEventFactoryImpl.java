package hub.ebb.jblcluster.eventservice.service.impl;

import hub.jbl.core.dto.FcjEventFactoryContainer;
import hub.jbl.core.dto.JpsEventFactoryContainer;
import hub.jbl.core.dto.OnStreetEventFactoryContainer;
import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.jbl.core.generator.InvalidTypeException;
import hub.jbl.eventservice.model.JblEventFactoryContainer;
import hub.jbl.eventservice.service.MainEventFactory;

public class MainEventFactoryImpl implements MainEventFactory{


    @Override
    public <T extends JpsEvent> T buildEvent(String eventSpecCode, String eventType) throws InvalidTypeException {

        Object event = null;

        try
        {
            event = ((JpsEventFactoryContainer)JpsEventFactoryContainer.getInstance()).jpsEventFactory(eventSpecCode,eventType);
        }
        catch (hub.jbl.core.generator.InvalidTypeException e)
        {

        }

        if(event == null)
        {
            try {
                event = ((JblEventFactoryContainer)JblEventFactoryContainer.getInstance()).jpsEventFactory(eventSpecCode,eventType);
            } catch (InvalidTypeException e) {

            }
        }

        if(event == null)
        {
            try {
                event = ((OnStreetEventFactoryContainer) OnStreetEventFactoryContainer.getInstance()).jpsEventFactory(eventSpecCode, eventType);
            } catch (InvalidTypeException e) {

            }
        }

        if (event == null)
        {
            try {
            event = ((FcjEventFactoryContainer)FcjEventFactoryContainer.getInstance()).jpsEventFactory(eventSpecCode, eventType);
            } catch (InvalidTypeException e) {

            }
        }

        return (T) event;

    }

    @Override
    public <T extends JpsEvent> T initEvent(T event) throws hub.jbl.core.generator.InvalidTypeException {

        T eventObj = null;

        try
        {
            eventObj = ((JpsEventFactoryContainer) JpsEventFactoryContainer.getInstance()).initEvent(event);
        }
        catch (hub.jbl.core.generator.InvalidTypeException e)
        {

        }

        if (eventObj == null)
        {
            try {
                eventObj = ((JblEventFactoryContainer) JblEventFactoryContainer.getInstance()).initEvent(event);
            } catch (hub.jbl.core.generator.InvalidTypeException e) {

            }
        }


        if(eventObj == null)
        {
            try {
                eventObj = ((OnStreetEventFactoryContainer)OnStreetEventFactoryContainer.getInstance()).initEvent(event);
            } catch (hub.jbl.core.generator.InvalidTypeException e) {

            }
        }


        if (eventObj == null)
        {
            try {
                eventObj = ((FcjEventFactoryContainer)FcjEventFactoryContainer.getInstance()).initEvent(event);
            } catch (hub.jbl.core.generator.InvalidTypeException e) {

            }
        }


        return eventObj;
    }
}
