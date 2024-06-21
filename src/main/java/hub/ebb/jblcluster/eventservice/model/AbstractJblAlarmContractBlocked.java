package hub.ebb.jblcluster.eventservice.model;

import hub.ebb.jblcluster.eventservice.mapper.JblVisitableEvent;
import hub.jbl.core.dto.jps.event.JpsEvtAlarm;

public abstract class AbstractJblAlarmContractBlocked extends JpsEvtAlarm implements JblVisitableEvent {
    private String contractNumber;

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }
}
