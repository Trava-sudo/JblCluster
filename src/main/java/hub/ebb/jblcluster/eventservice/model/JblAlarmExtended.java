package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.core.dto.jps.event.JpsAlarmPhase;
import hub.jbl.core.dto.jps.event.JpsEvent;
import hub.jbl.core.dto.jps.event.JpsEvtAlarm;
import hub.jbl.entity.events.JblAlarm;

public class JblAlarmExtended extends JblEventExtendedJbl {

    public static final String TABLE = "JBL_ALARM";

    @Override
    public JpsEvtAlarm getJpsEvent() {
        return (JpsEvtAlarm) super.getJpsEvent();
    }

    @Override
    public void setJpsEvent(JpsEvent jpsEvent) {
        if (jpsEvent instanceof JpsEvtAlarm) {
            super.setJpsEvent(jpsEvent);
        }
    }

    public JblAlarm getJblAlarm() {
        JblAlarm jblAlarm = new JblAlarm();
        jblAlarm.setJson(this.getJson());
        jblAlarm.setJmsJson(this.getJmsJson());
        jblAlarm.setJmsStatus(this.getJmsStatus());
        jblAlarm.setSequenceNumberTs(this.getSequenceNumberTs());
        jblAlarm.setSequenceNumberGmt(this.getSequenceNumberGmt());
        jblAlarm.setSequenceNumberCounter(this.getSequenceNumberCounter());
        jblAlarm.setPeripheralId(this.getPeripheralId());
        jblAlarm.setEventSpecCode(this.getEventSpecCode());
        jblAlarm.setEventType(this.getEventType());
        jblAlarm.setId(this.getId());
        jblAlarm.setCreationDate(this.getCreationDate());
        jblAlarm.setLastModificationDate(this.getLastModificationDate());
        jblAlarm.setVersion(this.getVersion());
        jblAlarm.setValid(this.getValid());
        jblAlarm.setSession(this.getSession());
        jblAlarm.setAlarmPhase(this.getAlarmPhase());
        return jblAlarm;
    }

    public static class Builder extends JblEventExtendedJbl.Builder<JblAlarmExtended, Builder> {

        public Builder(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
        }

        @Override
        public JblAlarmExtended createInstance() {
            return new JblAlarmExtended();
        }

        public Builder withPhase(JpsAlarmPhase phase) {
            getInstance().getJpsEvent().setAlarmPhase(phase);
            return this;
        }
    }
}
