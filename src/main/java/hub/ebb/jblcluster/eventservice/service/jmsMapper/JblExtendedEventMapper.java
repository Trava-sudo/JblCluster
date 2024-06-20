package hub.ebb.jblcluster.eventservice.service.jmsMapper;

import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.exception.JblMapperException;
import hub.jbl.core.dto.jps.event.JpsAlarmPhase;
import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.jbl.core.dto.jps.event.JpsEvtAlarm;
import hub.jbl.dao.common.JblMapper;
import hub.jbl.eventservice.model.JblAlarmExtended;
import hub.jbl.eventservice.model.JblEventExtendedJbl;
import hub.jbl.eventservice.model.JmsStatus;
import hub.jbl.eventservice.model.JpsSequenceNumber;

public class JblExtendedEventMapper extends JblMapper<JblEventExtendedJbl, JpsEvent> {

    @Override
    public JpsEvent toDto(JBLContext jblContext, JblEventExtendedJbl entity) {
        return null;
    }

    @Override
    public JblEventExtendedJbl toEntity(JBLContext jblContext, JpsEvent dto) throws JblMapperException {

        if (dto instanceof JpsEvtAlarm) {
            JblAlarmExtended jblAlarmExtended = new JblAlarmExtended();
            jblAlarmExtended.setJpsEvent(dto);
            jblAlarmExtended.setJmsStatus(JmsStatus.NOT_SENT);
            jblAlarmExtended.setSequenceNumber(JpsSequenceNumber.Now());
            final JpsAlarmPhase alarmPhase = ((JpsEvtAlarm) dto).getAlarmPhase();
            if (alarmPhase != null)
                jblAlarmExtended.setAlarmPhase(alarmPhase.toString());
            return jblAlarmExtended;
        } else {
            JblEventExtendedJbl jblEventExtended = new JblEventExtendedJbl();
            jblEventExtended.setJpsEvent(dto);
            jblEventExtended.setJmsStatus(JmsStatus.NOT_SENT);
            jblEventExtended.setSequenceNumber(JpsSequenceNumber.Now());
            return jblEventExtended;
        }
    }

    @Override
    public JblEventExtendedJbl mergeEntity(JBLContext jblContext, JpsEvent dto, JblEventExtendedJbl entity) {
        return null;
    }
}
