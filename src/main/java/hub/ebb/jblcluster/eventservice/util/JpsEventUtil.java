package hub.ebb.jblcluster.eventservice.util;

import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.date.DateUtils;
import hub.jbl.common.lib.log.Logger;
import hub.jbl.core.dto.jps.event.JpsAlarmPhase;
import hub.jbl.core.dto.jps.event.JpsEventSeverity;
import hub.jbl.core.dto.jps.event.JpsEvtAlarm;
import hub.jbl.core.dto.jps.event.SpecCodeEnum;
import hub.ebb.jblcluster.eventservice.service.MainEventFactory;

public class JpsEventUtil {
    private static Logger logger = JBLContext.getInstance().getLogger(JpsEventUtil.class);

    private JpsEventUtil() {
    }

    public static JpsEvtAlarm createAlarm(String eventSpecCode, String peripheralId, String peripheralType, JpsAlarmPhase jpsAlarmPhase, JpsEventSeverity jpsEventSeverity) {
        JpsEvtAlarm jpsEvtAlarm = null;
        MainEventFactory factoryContainer = MainEventFactory.getInstance();
        try {
            jpsEvtAlarm = factoryContainer.buildEvent(eventSpecCode, JpsEvtAlarm.DICTIONARY_TYPE_CODE);
        } catch (Exception e) {
            logger.error("Error", e);
            throw new RuntimeException(e);
        }
        jpsEvtAlarm.setEventSpecCode(SpecCodeEnum.forValue(eventSpecCode));
        jpsEvtAlarm.setEventType(JpsEvtAlarm.DICTIONARY_TYPE_CODE);
        jpsEvtAlarm.setSessionId(DateUtils.getUnixTSInMillis());
        jpsEvtAlarm.setEntityCode(peripheralId);
        jpsEvtAlarm.setAlarmPhase(jpsAlarmPhase);
        jpsEvtAlarm.setSeverity(jpsEventSeverity);
        jpsEvtAlarm.setPeripheralType(peripheralType);
        return jpsEvtAlarm;
    }
}
