package hub.ebb.jblcluster.eventservice.model;

import hub.ebb.jblcluster.eventservice.mapper.JblVisitableEvent;
import hub.jbl.core.dto.jps.event.JpsEvtAlarm;

public abstract class AbstractJblAlarmAccountBlocked extends JpsEvtAlarm implements JblVisitableEvent {
    private String accountDescription;

    public String getAccountDescription() {
        return accountDescription;
    }

    public void setAccountDescription(String accountDescription) {
        this.accountDescription = accountDescription;
    }
}
