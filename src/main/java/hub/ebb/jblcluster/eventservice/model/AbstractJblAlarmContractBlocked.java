package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.core.dto.jps.event.JpsEvtAlarm;
import hub.jbl.eventservice.mapper.JblVisitableEvent;

public abstract class AbstractJblAlarmContractBlocked extends JpsEvtAlarm implements JblVisitableEvent {
    private String contractNumber;

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }
}
