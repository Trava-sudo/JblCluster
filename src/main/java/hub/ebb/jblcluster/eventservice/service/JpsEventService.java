package hub.ebb.jblcluster.eventservice.service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import hub.ebb.jblcluster.eventservice.model.*;
import hub.ebb.jblcluster.eventservice.model.factory.InvalidJpsEventTypeException;
import hub.ebb.jblcluster.eventservice.service.jmsMapper.JblExtendedEventMapper;
import hub.jbl.common.dao.authentication.JpsAuthenticatedPeripheral;
import hub.jbl.common.exception.PrimayKeyDuplicatedException;
import hub.jbl.common.lib.JblAPIEventBusBundle;
import hub.jbl.common.lib.R;
import hub.jbl.common.lib.api.event.EventAPI;
import hub.jbl.common.lib.api.peripheral.JpsAuthenticatedPeripheralAPI;
import hub.jbl.common.lib.builder.AsyncResultBuilder;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.context.JBLContextImpl;
import hub.jbl.common.lib.date.DateUtils;
import hub.jbl.common.lib.date.JblDateTime;
import hub.jbl.common.lib.log.Logger;
import hub.jbl.common.lib.utils.serviceDiscovery.IServiceDiscoveryClient;
import hub.jbl.common.services.AbstractJblService;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.core.dto.jps.authentication.JblInternalPeripheral;
import hub.jbl.core.dto.jps.authentication.common.JpsPeripheral;
import hub.jbl.core.dto.jps.event.*;
import hub.jbl.core.generator.InvalidTypeException;
import hub.jbl.dao.*;
import hub.jbl.entity.events.JblAlarm;
import hub.jbl.entity.events.JblEvent;
import hub.jbl.entity.events.JblEventToContract;
import hub.jbl.entity.fiscal.FiscalPrinter;
import hub.jbl.entity.fiscal.FiscalPrinterEx;
import hub.jbl.entity.fiscal.FiscalPrinterStatusType;
import hub.jbl.entity.guestpass.JblGuestPass;
import hub.jms.common.model.account.invoice.PostPaymentMethodType;
import hub.jms.common.model.configuration.JblFiscalPrinterConfiguration;
import hub.jms.common.model.utils.JSONUtil;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Stefano.Coletta on 07/11/2016.
 */
public class JpsEventService extends AbstractJblService<JblEventExtendedJbl, JpsEvent> {

    private static final Logger logger = JBLContext.getInstance().getLogger(JpsEventService.class);
    private static EventAPI eventAPI;
    private static JpsAuthenticatedPeripheralAPI jpsAuthenticatedPeripheralAPI;
    private final String m_fiscalPrinterId_NA = "N/A";
    @Autowired
    private JblEventDao jblEventDAO;
    @Autowired
    private JblAlarmDao jblAlarmDAO;
    @Autowired
    private JblTransactionManager jblTransactionManager;
    @Autowired
    private FiscalPrinterDao fiscalPrinterDao;
    @Autowired
    private FiscalNumberCounterDao fiscalNumberCounterDao;
    @Autowired
    private Vertx vertx;
    @Autowired
    private JblEventToContractDao jblEventToContractDao;
    @Autowired
    private FcjOptorShiftDao fcjOptorShiftDao;

    public JpsEventService() {
        super(new JblExtendedEventMapper());
    }

    public static void sendAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, String peripheralId, String peripheralType, JpsAlarmPhase phase, Long sessionId, Map<String, String> additionalFields) throws InvalidJpsEventTypeException {

        JblAlarmExtended jblAlarm = new JblAlarmExtended();
        jblAlarm.setSequenceNumber(new JpsSequenceNumber(DateUtils.getUnixTSInMillis(), DateUtils.getGMTInMinutes(), EventSequenceNumberGenerator.getInstance().nextInt()));
        jblAlarm.setJmsStatus(JmsStatus.NOT_SENT);
        jblAlarm.setPeripheralId(peripheralId);

        JpsEvtAlarm jpsEvtAlarm = null;
        MainEventFactory factoryContainer = MainEventFactory.getInstance();
        try {
            jpsEvtAlarm = factoryContainer.buildEvent(eventSpecCode, JpsEvtAlarm.DICTIONARY_TYPE_CODE);
        } catch (hub.ebb.jblcluster.eventservice.generator.InvalidTypeException e) {
            JBLContext.getInstance().getLogger(JpsEventService.class).error(e);
        } catch (InvalidTypeException e) {
            JBLContext.getInstance().getLogger(JpsEventService.class).error(e);
        }
        jpsEvtAlarm.setEventSpecCode(SpecCodeEnum.forValue(eventSpecCode));
        jpsEvtAlarm.setEventType(JpsEvtAlarm.DICTIONARY_TYPE_CODE);
        jpsEvtAlarm.setSessionId(sessionId);
        jpsEvtAlarm.setEntityCode(peripheralId);
        jpsEvtAlarm.setAlarmPhase(phase);
        jpsEvtAlarm.setSeverity(JpsEventSeverity.Medium);
        jpsEvtAlarm.setPeripheralType(peripheralType);

        if (jpsEvtAlarm instanceof JblAlarmNewAuthenticatedDevice) {
            ((JblAlarmNewAuthenticatedDevice) jpsEvtAlarm).setPeripheralId(peripheralId);
        }
        if (jpsEvtAlarm instanceof JblAlarmGatelessOffline) {
            ((JblAlarmGatelessOffline) jpsEvtAlarm).setPeripheralId(peripheralId);
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        }
        if (jpsEvtAlarm instanceof JblAlarmLPRMissmatch) {

            ((JblAlarmLPRMissmatch) jpsEvtAlarm).setCardNumber(additionalFields.get("cardNumber"));
            ((JblAlarmLPRMissmatch) jpsEvtAlarm).setEntryLpr(additionalFields.get("entryLpr"));
            ((JblAlarmLPRMissmatch) jpsEvtAlarm).setExitLpr(additionalFields.get("exitLpr"));
        }

        if (jpsEvtAlarm instanceof JblAlarmCardPoolSoftLimitExceeded) {
            ((JblAlarmCardPoolSoftLimitExceeded) jpsEvtAlarm).setCardPoolName(additionalFields.get("cardPoolName"));
            ((JblAlarmCardPoolSoftLimitExceeded) jpsEvtAlarm).setCardPoolActualValue(Integer.parseInt(additionalFields.get("cardPoolActualValue")));
            ((JblAlarmCardPoolSoftLimitExceeded) jpsEvtAlarm).setCardPoolSoftLimitValue(Integer.parseInt(additionalFields.get("cardPoolSoftLimitValue")));
        }

        if (jpsEvtAlarm instanceof JblTimePeripheralMismatch && additionalFields != null) {
            ((JblTimePeripheralMismatch) jpsEvtAlarm).setPeripheralId(additionalFields.get("peripheralId"));
            ((JblTimePeripheralMismatch) jpsEvtAlarm).setDelay(Integer.parseInt(additionalFields.get("delay")));
        }

        if (jpsEvtAlarm instanceof JblAlarmExpiredSubscriptionEntryWithTicket && additionalFields != null) {
            ((JblAlarmExpiredSubscriptionEntryWithTicket) jpsEvtAlarm).setPlate(additionalFields.get("PLATE"));
            if (!Strings.isNullOrEmpty(additionalFields.get("JMSOPERATIONID")))
                jpsEvtAlarm.setJmsOperationId(Long.parseLong(additionalFields.get("JMSOPERATIONID")));
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        }

        if (jpsEvtAlarm instanceof JblAlarmTypeShift && additionalFields != null) {
            ((JblAlarmTypeShift) jpsEvtAlarm).setOldPeripheralId(additionalFields.get("peripheralId"));
        }

        if (jpsEvtAlarm instanceof JblAlarmPeripheralOffline) {
            ((JblAlarmPeripheralOffline) jpsEvtAlarm).setPeripheralId(peripheralId);
            if (additionalFields != null) {
                ((JblAlarmPeripheralOffline) jpsEvtAlarm).setPeripheralName(additionalFields.get("peripheralName"));
            }
        }

        if (jpsEvtAlarm instanceof JblAlarmFixedPathTimeViolation) {
            ((JblAlarmFixedPathTimeViolation) jpsEvtAlarm).setCardNumber(additionalFields.get("card"));
        }

        if (jpsEvtAlarm instanceof JblAlarmVehicleClassMismatch) {
            ((JblAlarmVehicleClassMismatch) jpsEvtAlarm).setUid(additionalFields.get("uid"));
            ((JblAlarmVehicleClassMismatch) jpsEvtAlarm).setVehicleClass(additionalFields.get("vehicleClass"));
            ((JblAlarmVehicleClassMismatch) jpsEvtAlarm).setVehicleClassDetected(additionalFields.get("vehicleClassDetected"));
        }

        if (jpsEvtAlarm instanceof JblAlarmLPRBlocklist && additionalFields != null) {
            ((JblAlarmLPRBlocklist) jpsEvtAlarm).setPlate(additionalFields.get("PLATE"));
            ((JblAlarmLPRBlocklist) jpsEvtAlarm).setDeviceName(additionalFields.get("DEVICENAME"));
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        }

        if (jpsEvtAlarm instanceof JblAlarmLPRWarninglist && additionalFields != null) {
            ((JblAlarmLPRWarninglist) jpsEvtAlarm).setPlate(additionalFields.get("PLATE"));
            ((JblAlarmLPRWarninglist) jpsEvtAlarm).setDeviceName(additionalFields.get("DEVICENAME"));
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        }

        if (jpsEvtAlarm instanceof JblAlarmMembershipTransitNoPlate && additionalFields != null) {
            ((JblAlarmMembershipTransitNoPlate) jpsEvtAlarm).setContractNumber(additionalFields.get("CONTRACT_NUMBER"));
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        }

        if (jpsEvtAlarm instanceof JblAlarmLUnipolNotAccessible && additionalFields != null) {
            ((JblAlarmLUnipolNotAccessible) jpsEvtAlarm).setPlate(additionalFields.get("PLATE"));
            ((JblAlarmLUnipolNotAccessible) jpsEvtAlarm).setDeviceName(additionalFields.get("DEVICENAME"));
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        }

        jblAlarm.setJpsEvent(jpsEvtAlarm);

        final String evt = jpsEvtAlarm.toString();

        clientPostEvent(jblContext, null, serviceDiscoveryClient, jblAlarm, false, voidAsyncResult -> {

            if (voidAsyncResult.succeeded()) {
                logger.info(evt + " correctly sent");
            } else {
                logger.info("Error on sending alarm to JpsEventService", voidAsyncResult.cause());
            }
        });
    }

    public static void sendOneShotTakeInCareAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, String peripheralId, String peripheralType, Long sessionId) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, peripheralId, peripheralType, JpsAlarmPhase.ONESHOT_TAKE_IN_CARE, sessionId, null);
    }

    public static void sendOneShotTakeInCareAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, Long sessionId) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, "JBL", "JBL", JpsAlarmPhase.ONESHOT_TAKE_IN_CARE, sessionId, null);
    }

    public static void sendOneShotTakeInCareAlarmWithFields(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, Long sessionId, Map<String, String> additionalFields) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, "JBL", "JBL", JpsAlarmPhase.ONESHOT_TAKE_IN_CARE, sessionId, additionalFields);
    }

    public static void sendOneShotAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, String peripheralId, String peripheralType, Long sessionId) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, peripheralId, peripheralType, JpsAlarmPhase.ONESHOT, sessionId, null);
    }

    public static void sendOneShotAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, Long sessionId, Map<String, String> additionalsField) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, "JBL", "JBL", JpsAlarmPhase.ONESHOT, sessionId, additionalsField);
    }

    public static void sendOneShotAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, Long sessionId) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, "JBL", "JBL", JpsAlarmPhase.ONESHOT, sessionId, null);
    }

    public static void sendOneShotAlarmWithFields(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, String peripheralId, String peripheralType, Long sessionId, Map<String, String> additionalField) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, peripheralId, peripheralType, JpsAlarmPhase.ONESHOT, sessionId, additionalField);
    }

    public static void sendClearableAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, String peripheralId, String peripheralType, Long sessionId) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, peripheralId, peripheralType, JpsAlarmPhase.START, sessionId, null);
    }

    public static void sendClearableAlarm(JBLContext jblContext, IServiceDiscoveryClient iServiceDiscoveryClient, JpsEvtAlarm jpsEvtAlarm, String peripheralId, String peripheralType, Long sessionId) throws InvalidJpsEventTypeException {

        JblAlarmExtended jblAlarm = new JblAlarmExtended();
        jblAlarm.setSequenceNumber(new JpsSequenceNumber(DateUtils.getUnixTSInMillis(), DateUtils.getGMTInMinutes(), EventSequenceNumberGenerator.getInstance().nextInt()));
        jblAlarm.setJmsStatus(JmsStatus.NOT_SENT);
        jblAlarm.setPeripheralId(peripheralId);

        jpsEvtAlarm.setSessionId(sessionId);
        jpsEvtAlarm.setAlarmPhase(JpsAlarmPhase.START);
        jpsEvtAlarm.setPeripheralType(peripheralType);

        jpsEvtAlarm.setEventSpecCode(SpecCodeEnum.forValue(jpsEvtAlarm.getClass().getSimpleName()));
        jpsEvtAlarm.setEventType(JpsEvtAlarm.DICTIONARY_TYPE_CODE);
        jpsEvtAlarm.setSeverity(JpsEventSeverity.Medium);
        jblAlarm.setJpsEvent(jpsEvtAlarm);

        clientPostEvent(jblContext, null, iServiceDiscoveryClient, jblAlarm, false, voidAsyncResult -> {

            if (voidAsyncResult.succeeded()) {
                logger.info(jpsEvtAlarm + " correctly sent");
            } else {
                logger.info("Error on sending alarm to JpsEventService", voidAsyncResult.cause());
            }
        });
    }

    public static void clearAlarm(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, String eventSpecCode, String peripheralId, String peripheralType, Long sessionId) throws InvalidJpsEventTypeException {
        sendAlarm(jblContext, serviceDiscoveryClient, eventSpecCode, peripheralId, peripheralType, JpsAlarmPhase.END, sessionId, null);
    }

    public static void clientPostEventAtSequenceNumberTime(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, JpsEvent event, JpsSequenceNumber sequenceNumber, JpsAuthenticatedPeripheral authenticatedPeripheral, Handler<AsyncResult<Void>> resultHandler) {
        if (event.getSessionId() == null) {
            event.setSessionId(0L);
        }

        JblEventExtendedJbl jblEvent = new JblEventExtendedJbl.Builder<>(event.getEventSpecCode().getValue(), event.getEventType())
                .withJmsStatus(JmsStatus.NOT_SENT)
                .withPeripheralId(authenticatedPeripheral.getPeripheralId())
                .withPeripheralType(authenticatedPeripheral.getType())
                .withSequenceNumberTS(sequenceNumber.getDateTime().getTimestamp())
                .withSequenceNumberGMT(sequenceNumber.getDateTime().getGmt())
                .withSessionId(event.getSessionId())
                .withSequenceNumberCounter(sequenceNumber.getCounter()).build();

        jblEvent.setJpsEvent(event);

        clientPostEvent(jblContext, authenticatedPeripheral.getToken(), serviceDiscoveryClient, jblEvent, true, resultHandler);
    }

    public static void clientPostEventAtSequenceNumberTime(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, JpsEvent event, JpsSequenceNumber sequenceNumber, Handler<AsyncResult<Void>> resultHandler) {


        if (event.getSessionId() == null) {
            event.setSessionId(0L);
        }

        JblEventExtendedJbl jblEvent = new JblEventExtendedJbl.Builder<>(event.getEventSpecCode().getValue(), event.getEventType())
                .withJmsStatus(JmsStatus.NOT_SENT)
                .withPeripheralId(new JblInternalPeripheral().getPeripheral().getPeripheralId())
                .withPeripheralType(new JblInternalPeripheral().getPeripheral().getPeripheralType())
                .withSequenceNumberTS(sequenceNumber.getDateTime().getTimestamp())
                .withSequenceNumberGMT(sequenceNumber.getDateTime().getGmt())
                .withSessionId(event.getSessionId())
                .withSequenceNumberCounter(sequenceNumber.getCounter()).build();

        jblEvent.setJpsEvent(event);


        clientPostEvent(jblContext, null, serviceDiscoveryClient, jblEvent, false, resultHandler);
    }

    public static void clientPostEventAtSequenceNumberTimeAsPeripheral(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, JpsEvent event, String peripheralId, String authToken, JpsSequenceNumber sequenceNumber, Handler<AsyncResult<Void>> resultHandler) {
        if (event.getSessionId() == null) {
            event.setSessionId(0L);
        }

        JblEventExtendedJbl jblEvent = new JblEventExtendedJbl.Builder<>(event.getEventSpecCode().getValue(), event.getEventType())
                .withJmsStatus(JmsStatus.NOT_SENT)
                .withPeripheralId(peripheralId)
                .withPeripheralType(event.getPeripheralType())
                .withSequenceNumberTS(sequenceNumber.getDateTime().getTimestamp())
                .withSequenceNumberGMT(sequenceNumber.getDateTime().getGmt())
                .withSessionId(event.getSessionId())
                .withSequenceNumberCounter(sequenceNumber.getCounter()).build();

        jblEvent.setJpsEvent(event);


        clientPostEvent(jblContext, authToken, serviceDiscoveryClient, jblEvent, false, resultHandler);
    }

    public static void clientPostEvent(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, JblEventExtendedJbl event, boolean safe, Handler<AsyncResult<Void>> resultHandler) {

        JpsEvent jpsEvent = event.getJpsEvent();

        if (jpsEvent.getSessionId() == null) {
            jpsEvent.setSessionId(0L);
        }
        if (StringUtils.isEmpty(jpsEvent.getEntityCode())) {
            jpsEvent.setEntityCode("JBL");
        }

        event.setJpsEvent(jpsEvent);

        clientPostEvent(jblContext, null, serviceDiscoveryClient, event, safe, resultHandler);
    }

    @Deprecated
    public static void clientPostEvent(JBLContext jblContext, IServiceDiscoveryClient serviceDiscoveryClient, JpsEvent event, boolean safe, Handler<AsyncResult<Void>> resultHandler) {

        if (event.getSessionId() == null) {
            event.setSessionId(0L);
        }
        if (StringUtils.isEmpty(event.getEntityCode())) {
            event.setEntityCode("JBL");
        }
        JblEventExtendedJbl jblEvent = new JblEventExtendedJbl.Builder<>(event.getEventSpecCode().getValue(), event.getEventType())
                .withJmsStatus(JmsStatus.NOT_SENT)
                .withPeripheralId(new JblInternalPeripheral().getPeripheral().getPeripheralId())
                .withPeripheralType(new JblInternalPeripheral().getPeripheral().getPeripheralType())
                .withSequenceNumberTS(JblDateTime.now().getTimestamp())
                .withSequenceNumberGMT(JblDateTime.now().getGmt())
                .withSessionId(event.getSessionId())
                .withSequenceNumberCounter(EventSequenceNumberGenerator.getInstance().nextInt()).build();

        jblEvent.setJpsEvent(event);


        clientPostEvent(jblContext, null, serviceDiscoveryClient, jblEvent, safe, resultHandler);
    }

    public static void clientPostEventAsPeripheral(JBLContext jblContext, MultiMap headers, JblEventExtendedJbl event, String authToken, Handler<AsyncResult<Void>> resultHandler) {

        if (StringUtils.isEmpty(event.getPeripheralId()) && StringUtils.isEmpty(event.getJpsEvent().getPeripheralType())) {
            event.setPeripheralId(event.getPeripheralId());
            event.getJpsEvent().setPeripheralType(event.getJpsEvent().getPeripheralType());
        }

        send(jblContext, headers, event, authToken, false, resultHandler);
    }

    public static void sendSuccessfulPayment(JBLContext context, PeripheralTypeValues peripheralType, JpsLogUsrPass usrPass, JpsSequenceNumber sequenceNumber, IServiceDiscoveryClient serviceDiscoveryClient, Long sessionId, String peripheralId, String authToken, BigDecimal operationAmount, BigDecimal payedAmount, Handler<AsyncResult<Void>> resultHandler) {
        JpsOpUsrPayCompl payCompl = new JpsOpUsrPayCompl();
        try {
            payCompl = MainEventFactory.getInstance().initEvent(payCompl);
        } catch (hub.ebb.jblcluster.eventservice.generator.InvalidTypeException e) {
            e.printStackTrace();
        } catch (InvalidTypeException e) {
            e.printStackTrace();
        }

        payCompl.setUsrPass(usrPass);
        payCompl.setCurrency("EUR");
        payCompl.setPrepayed(false);
        payCompl.setPrepayedEndDateTimeTs(null);
        payCompl.setEventSpecCode(SpecCodeEnum.JpsOpUsrPayCompl);
        payCompl.setSessionId(sessionId);
        payCompl.setPeripheralType(peripheralType.getValue());
        payCompl.setEntityCode(peripheralId);
        payCompl.setSeverity(JpsEventSeverity.Medium);
        payCompl.setReqAmount(operationAmount);

        JpsPaymentData paymentData = new JpsPaymentData();
        paymentData.setError(JpsPaymentError.ErrNone);
        paymentData.setAcqAmount(payedAmount);
        paymentData.setCngAmount(BigDecimal.ZERO);
        paymentData.setSubtype(JblPaymentDataSubtype.JBL_PAYMENT_DATA_SUBTYPE_NONE);

        payCompl.setData(paymentData);

        clientPostEventAtSequenceNumberTimeAsPeripheral(context, serviceDiscoveryClient, payCompl, peripheralId, authToken, sequenceNumber, resultHandler);

    }

    public static void sendGuestPassPayment(JBLContext context, JpsLogUsrPass usrPass, JblGuestPass guestPass,
                                            IServiceDiscoveryClient serviceDiscoveryClient, BigDecimal operationAmount,
                                            Integer overnightsDiff, String operatorName, Handler<AsyncResult<Void>> resultHandler) {
        JblGuestPassChangeEvent payCompl = new JblGuestPassChangeEvent();

        try {
            payCompl = MainEventFactory.getInstance().initEvent(payCompl);
        } catch (hub.ebb.jblcluster.eventservice.generator.InvalidTypeException | InvalidTypeException e) {
            e.printStackTrace();
        }

        payCompl.setUsrPass(usrPass);
        payCompl.setCurrency("EUR");
        payCompl.setPrepayed(false);
        payCompl.setPrepayedEndDateTimeTs(null);
        payCompl.setEventSpecCode(usrPass.getEventSpecCode());
        payCompl.setEventType(R.PAYMENT);
        payCompl.setType(PostPaymentMethodType.BANK_TRANSFER);
        payCompl.setSessionId(0L);
        payCompl.setPeripheralType(JblInternalPeripheral.Peripheral().getPeripheralType());
        payCompl.setEntityCode(JblInternalPeripheral.Peripheral().getPeripheralId());
        payCompl.setSeverity(JpsEventSeverity.Medium);
        payCompl.setReqAmount(operationAmount);
        payCompl.setOvernightsDiff(overnightsDiff);
        payCompl.setOvernights(guestPass.getOvernights());
        payCompl.setPeriodFrom(guestPass.getCheckInTime().getTimestamp());
        payCompl.setPeriodFromGmt((short)guestPass.getCheckInTime().getGmt());
        payCompl.setPeriodTo(guestPass.getCheckOutTime().getTimestamp());
        payCompl.setPeriodToGmt((short)guestPass.getCheckOutTime().getGmt());

        JpsPaymentData paymentData = new JpsPaymentData();
        paymentData.setError(JpsPaymentError.ErrNone);
        paymentData.setAcqAmount(BigDecimal.ZERO);
        paymentData.setCngAmount(BigDecimal.ZERO);
        paymentData.setSubtype(JblPaymentDataSubtype.JBL_PAYMENT_DATA_SUBTYPE_NONE);
        paymentData.setOperatorId(operatorName);

        payCompl.setData(paymentData);

        clientPostEventAtSequenceNumberTimeAsPeripheral(context, serviceDiscoveryClient, payCompl,
                usrPass.getPeripheralId(), JblInternalPeripheral.INTERNAL_TOKEN, JpsSequenceNumber.Now(), resultHandler);

    }

    public static void sendForceTransit(JBLContext context, IServiceDiscoveryClient serviceDiscoveryClient, JpsAuthenticatedPeripheral authenticatedPeripheral, JpsLogUsrPass usrPass, JpsSequenceNumber sequenceNumber, BigDecimal deductedAmount, Handler<AsyncResult<Void>> resultHandler) {
        Logger logger = context.getLogger(JpsEventService.class);
        JpsOpUsrTransit transit = new JpsOpUsrTransit();
        try {
            transit = MainEventFactory.getInstance().initEvent(transit);
        } catch (hub.ebb.jblcluster.eventservice.generator.InvalidTypeException | InvalidTypeException e) {
            logger.error("ERROR SENDING SUCCESSFUL EXIT", e);
            throw new RuntimeException(e);
        }
        usrPass.setPeripheralId(authenticatedPeripheral.getPeripheralId());
        usrPass.setPeripheralType(authenticatedPeripheral.getType());
        usrPass.setEventSpecCode(SpecCodeEnum.JpsOpUsrTransit);
        transit.setCcData(usrPass.getCcData());
        transit.setUsrPass(usrPass);
        transit.setEventSpecCode(SpecCodeEnum.JpsOpUsrTransit);
        transit.setSessionId(0L);
        transit.setPeripheralType(authenticatedPeripheral.getType());
        transit.setEntityCode(authenticatedPeripheral.getPeripheralId());
        transit.setSeverity(JpsEventSeverity.Medium);
        transit.setTransitType(JpsTransitType.Force);
        transit.setVehicleCategory(usrPass.getVehicleCategory());
        if (deductedAmount != null)
            transit.setDeductedAmount(deductedAmount.doubleValue());
        clientPostEventAtSequenceNumberTimeAsPeripheral(context, serviceDiscoveryClient, transit, authenticatedPeripheral.getPeripheralId(), authenticatedPeripheral.getToken(), sequenceNumber, resultHandler);

    }

    public static void sendVoidEntry(JBLContext context, IServiceDiscoveryClient serviceDiscoveryClient, JpsAuthenticatedPeripheral authenticatedPeripheral, JpsLogUsrPass usrPass, JpsSequenceNumber sequenceNumber, Handler<AsyncResult<Void>> resultHandler) {
        Logger logger = context.getLogger(JpsEventService.class);
        JpsOpUsrTransit transit = new JpsOpUsrTransit();
        try {
            transit = MainEventFactory.getInstance().initEvent(transit);
        } catch (hub.ebb.jblcluster.eventservice.generator.InvalidTypeException | InvalidTypeException e) {
            logger.error("ERROR SENDING VOID ENTRY", e);
            throw new RuntimeException(e);
        }
        usrPass.setPeripheralId(authenticatedPeripheral.getPeripheralId());
        usrPass.setPeripheralType(authenticatedPeripheral.getType());
        usrPass.setEventSpecCode(SpecCodeEnum.JpsOpUsrTransit);
        transit.setCcData(usrPass.getCcData());
        transit.setUsrPass(usrPass);
        transit.setEventSpecCode(SpecCodeEnum.JpsOpUsrTransit);
        transit.setSessionId(0L);
        transit.setPeripheralType(authenticatedPeripheral.getType());
        transit.setEntityCode(authenticatedPeripheral.getPeripheralId());
        transit.setSeverity(JpsEventSeverity.Medium);
        transit.setTransitType(JpsTransitType.Void);
        transit.setVehicleCategory(usrPass.getVehicleCategory());
        clientPostEventAtSequenceNumberTimeAsPeripheral(context, serviceDiscoveryClient, transit, authenticatedPeripheral.getPeripheralId(), authenticatedPeripheral.getToken(), sequenceNumber, resultHandler);
    }

    public static void clientPostEvent(JBLContext jblContext, MultiMap headers, String authToken, IServiceDiscoveryClient serviceDiscoveryClient, JblEventExtendedJbl event, boolean safe, Handler<AsyncResult<Void>> resultHandler) {

        if (StringUtils.isEmpty(event.getPeripheralId()) && StringUtils.isEmpty(event.getJpsEvent().getPeripheralType())) {
            event.setPeripheralId(R.INTERNAL_PERIPHERAL_ID);
            event.getJpsEvent().setPeripheralType(R.INTERNAL_PERIPHERAL_ID);
        }

        send(jblContext, headers, event, Objects.requireNonNullElse(authToken, JblInternalPeripheral.INTERNAL_TOKEN), safe, resultHandler);
    }

    public static void clientPostEvent(JBLContext jblContext, String authToken, IServiceDiscoveryClient serviceDiscoveryClient, JblEventExtendedJbl event, boolean safe, Handler<AsyncResult<Void>> resultHandler) {
        clientPostEvent(jblContext, null, authToken, serviceDiscoveryClient, event, safe, resultHandler);
    }

    private static void send(JBLContext jblContext, MultiMap headers, JblEventExtendedJbl event, String authToken, boolean safe, Handler<AsyncResult<Void>> resultHandler) {

        if (headers != null) {
            List<Map.Entry<String, String>> entryList = headers.entries();
            for (Map.Entry<String, String> entry : entryList) {
                String key = cleanKey(entry.getKey());
                jblContext.addSessionField(key, entry.getValue());
            }
        }

        eventAPI.doEventManagement(JSONUtil.serialize(event.getJpsEvent()),
                event.getSequenceNumber().getDateTime().getTimestamp(),
                event.getSequenceNumber().getDateTime().getGmt(),
                event.getSequenceNumber().getCounter(),
                authToken,
                event.getPeripheralId(),
                jblContext.serializeJpsSession(), safe, l -> {
                    if (200 == l.result()) {
                        if (resultHandler != null) {
                            resultHandler.handle(Future.succeededFuture());
                        }
                    } else if (500 == l.result()) {
                        if (resultHandler != null) {
                            resultHandler.handle(Future.failedFuture(new Exception("generic.error")));
                        }
                    } else {
                        if (resultHandler != null) {
                            resultHandler.handle(Future.failedFuture(""));
                        }
                    }
                });
    }

    public void getAllAuthenticatedPeripheralsViaProxy(){
        jpsAuthenticatedPeripheralAPI.getAllPeripherals(listAsyncResult -> {
            if (listAsyncResult.succeeded()) {
                listAsyncResult.result().forEach(peripheral -> {
                    logger.info("Peripheral " + peripheral.getJsonObject("peripheralId") + " of type " + peripheral.getJsonObject("type") + " found.");
                });
            } else {
                logger.error("Error retrieving peripherals.");
            }

        });
    }

    private static String cleanKey(String key) {
        try {
            if (key.startsWith(JBLContextImpl.JBL_CUSTOM_HEADER_PREFIX)) {
                return key.split(JBLContextImpl.JBL_CUSTOM_HEADER_PREFIX)[1];
            }
        } catch (Exception e) {

        }
        return key;

    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        eventAPI = EventAPI.createProxy(vertx);
        jpsAuthenticatedPeripheralAPI = JpsAuthenticatedPeripheralAPI.createProxy(vertx, JpsAuthenticatedPeripheralAPI.ADDRESS);
    }

    public void changePrinterStatus(JBLContext context, String peripheralId, FiscalPrinterStatusType fpStatus) {

        jblTransactionManager.executeTransaction((conn, transactionBodyHandler) -> {
            FiscalPrinter example = new FiscalPrinter();
            example.setPeripheralId(peripheralId);
            fiscalPrinterDao.findByExample(context, conn, example).thenAccept(listAsyncResult -> {
                if (listAsyncResult.succeeded()) {
                    if (!listAsyncResult.result().isEmpty()) {
                        FiscalPrinter fp = null;

                        fp = listAsyncResult.result().get(0);

                        String currentStatus = fp.getStatus();
                        String fpId = fp.getFiscalPrinterId();

                        fp.setStatus(fpStatus.name());

                        //EBBS-1172
                        if (!fp.getStatus().equals(currentStatus)) {
                            sendEventBusFiscalPrinterHistory(fp);
                        }

                        fiscalPrinterDao.updateStatus(context, conn, fp).thenAccept(fiscalPrinterAsyncResult -> {
                            if (fiscalPrinterAsyncResult.succeeded()) {
                                transactionBodyHandler.handle(Future.succeededFuture());
                            } else {
                                logger.error("EBBS-1170: Error update status of fiscal printer id: '" + fpId + "', peripheralId: '" + peripheralId + "'.");
                                transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                            }
                        });
                    } else {
                        transactionBodyHandler.handle(Future.succeededFuture());
                    }
                } else {
                    transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                }
            });
        });
    }

    public void changeOrCreatePrinterStatus(JBLContext context, String peripheralId, JblFiscalPrinterConfiguration configuration) {
        logger.info("EBBS-1170: Processing '" + peripheralId + "'.");
        String confFiscalPrinterId = m_fiscalPrinterId_NA;
        if (configuration.getFiscalPrinterId() != null && configuration.getFiscalPrinterId().length() != 0) {
            confFiscalPrinterId = configuration.getFiscalPrinterId();
        }
        String finalFiscalPrinterId = confFiscalPrinterId;

        FiscalPrinterStatusType fpStatus = FiscalPrinterStatusType.Available;
        if (finalFiscalPrinterId.equals(m_fiscalPrinterId_NA) ||
                configuration.getStatusText().contains("CoverOpen") ||
                configuration.getStatusText().contains("GeneralError") ||
                configuration.getStatusText().contains("PaperEnd") ||
                configuration.getStatusText().contains("ErrQueryingStatus") ||
                configuration.getStatusText().isEmpty()) {
            fpStatus = FiscalPrinterStatusType.Unavailable;
        }
        String finalStatus = fpStatus.name();

        jblTransactionManager.executeTransaction((conn, transactionBodyHandler) -> {
            FiscalPrinter example = new FiscalPrinter();
            example.setPeripheralId(peripheralId);
            fiscalPrinterDao.findByExample(context, conn, example).thenAccept(listAsyncResult -> {
                if (listAsyncResult.succeeded()) {
                    logger.info("EBBS-1170: FindByExample succeeded");
                    if (!listAsyncResult.result().isEmpty()) {
                        logger.info("EBBS-1170: FindByExample has " + listAsyncResult.result().size() + " results");

                        FiscalPrinter fp = null;
                        fp = listAsyncResult.result().get(0);

                        String currentStatus = fp.getStatus();
                        String currentFiscalPrinter = fp.getFiscalPrinterId();

                        fp.setStatus(finalStatus);

                        if (!finalFiscalPrinterId.equals(m_fiscalPrinterId_NA)) {
                            fp.setFiscalPrinterId(finalFiscalPrinterId);
                        }

                        if ("true".equalsIgnoreCase(configuration.getPresent())) {
                            logger.info("EBBS-1170: Found '" + finalStatus + "' fiscal printer for '" + fp.getPeripheralId() + "' with id '" + finalFiscalPrinterId + "'.");
                        } else {
                            fp.setStatus(FiscalPrinterStatusType.Unavailable.name());
                            logger.info("EBBS-1170: Found Unavailable fiscal printer for '" + fp.getPeripheralId() + "' with id '" + finalFiscalPrinterId + "'.");
                        }

                        // EBBS-1172
                        if (!fp.getStatus().equals(currentStatus) || !fp.getFiscalPrinterId().equals(currentFiscalPrinter)) {
                            sendEventBusFiscalPrinterHistory(fp);
                        }

                        fiscalPrinterDao.updateFP(context, conn, fp).thenAccept(fiscalPrinterAsyncResult -> {
                            if (fiscalPrinterAsyncResult.succeeded()) {
                                if (!finalFiscalPrinterId.equals(m_fiscalPrinterId_NA) && !finalFiscalPrinterId.equals(currentFiscalPrinter)) {
                                    fiscalNumberCounterDao.addFiscalPrinterId(context, conn, finalFiscalPrinterId, event -> {
                                        if (event.succeeded()) {
                                            logger.info("EBBS-1170: Added Fiscal printer counter, printerId: '" + finalFiscalPrinterId + "'.");
                                            transactionBodyHandler.handle(Future.succeededFuture());
                                        } else {
                                            logger.info("EBBS-1170: Error adding fiscal printer counter, printerId: '" + finalFiscalPrinterId + "'.");
                                            transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                                        }
                                    });
                                } else {
                                    transactionBodyHandler.handle(Future.succeededFuture());
                                }
                            } else {
                                transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                            }
                        });
                    } else {
                        logger.info("EBBS-1170: FindByExample returned empty collection");
                        try {
                            if ("true".equalsIgnoreCase(configuration.getPresent()) && !finalFiscalPrinterId.equals(m_fiscalPrinterId_NA)) {
                                FiscalPrinter fp = new FiscalPrinter();
                                fp.setStatus(finalStatus);
                                fp.setFiscalPrinterId(finalFiscalPrinterId);
                                fp.setPeripheralId(peripheralId);

                                logger.info("EBBS-1170: Found '" + finalStatus + "' fiscal printer for '" + peripheralId + "' with id '" + finalFiscalPrinterId + "'.");
                                fiscalPrinterDao.createFP(context, conn, fp).thenAccept(fiscalPrinterAsyncResult -> {
                                    if (fiscalPrinterAsyncResult.succeeded()) {
                                        fiscalNumberCounterDao.addFiscalPrinterId(context, conn, finalFiscalPrinterId, event -> {

                                            sendEventBusFiscalPrinterHistory(fp);

                                            if (event.succeeded()) {
                                                logger.info("EBBS-1170: Fiscal printer added, peripheralId: '" + peripheralId + "', printerId: '" + finalFiscalPrinterId + "'.");
                                                transactionBodyHandler.handle(Future.succeededFuture());
                                            } else {
                                                logger.info("EBBS-1170: Error adding fiscal printer, peripheralId: '" + peripheralId + "', printerId: '" + finalFiscalPrinterId + "'.");
                                                transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                                            }
                                        });
                                    } else {
                                        logger.info("EBBS-1170: Error saving fiscal printer, peripheralId: '" + peripheralId + "', printerId: '" + finalFiscalPrinterId + "'.");
                                        transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                                    }
                                });
                            } else {
                                transactionBodyHandler.handle(Future.succeededFuture());
                                logger.info("EBBS-1170: Found device '" + peripheralId + "' with no configured fiscsl printer id: '" + finalFiscalPrinterId + "'.");
                            }
                        } catch (Exception e) {
                            transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                            logger.error("EBBS-1170: Error: ");
                            e.printStackTrace();
                            logger.error(e.getMessage(), e);
                            logger.error(e.getMessage(), e.getCause());
                        }
                    }
                } else {
                    logger.info("EBBS-1170: findByExample failed");
                    transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                }
            });
        });
    }


    private void sendEventBusFiscalPrinterHistory(FiscalPrinter fp) {

        JBLContext context = JBLContext.getInstance();

        FiscalPrinterEx fpEx = new FiscalPrinterEx();
        fpEx.setPeripheralId(fp.getPeripheralId());
        fpEx.setFiscalPrinterId(m_fiscalPrinterId_NA);
        if (fp.getFiscalPrinterId() != null && fp.getFiscalPrinterId().length() != 0) {
            fpEx.setFiscalPrinterId(fp.getFiscalPrinterId());
        }
        fpEx.setStatus(fp.getStatus());

        jblTransactionManager.executeTransaction((connection, resultHandler) -> {
            fiscalNumberCounterDao.getCurrentNumberForFPId(context, connection, fpEx.getFiscalPrinterId(), getCurrNumberAsyncResult -> {
                if (getCurrNumberAsyncResult.succeeded() && getCurrNumberAsyncResult.result() != null) {
                    fpEx.setCurrentPrinterCounter(getCurrNumberAsyncResult.result());
                }

                try {
                    logger.info("EBBS-1172: Send to JMS '" + fpEx.getStatus() + "' status for '" + fpEx.getPeripheralId() + "' with fiscal printer id '" + fpEx.getFiscalPrinterId() + "'.");
                    vertx.eventBus().request(JblAPIEventBusBundle.PUB_FISCPRINTER_HISTORY, JSONUtil.serialize(fpEx));
                } catch (Exception e) {
                    logger.error("EBBS-1172: Send to JMS '" + fpEx.getStatus() + "' status for '" + fpEx.getPeripheralId() + "' with fiscal printer id '" + fpEx.getFiscalPrinterId() + "'. " + e.getMessage());
                }

                if (getCurrNumberAsyncResult.succeeded()) {
                    logger.info("EBBS-1172: Get Current Number for Fiscal printer succeeded.");
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    logger.error("EBBS-1172: Error get Current Number for Fiscal printer id: '" + fpEx.getFiscalPrinterId() + "'");
                    context.getLogger(this.getClass()).error("EBBS-1172: DB ERROR", getCurrNumberAsyncResult.cause());
                    resultHandler.handle(new AsyncResultBuilder<>().withFail().build());
                }
            });
        });
    }

    public CompletableFuture<AsyncResult<JblEventExtendedJbl>> insertAlarm(JBLContext context, JpsEvtAlarm alarmDTO, JpsSequenceNumber sequenceNumber, String peripheralId, String session) {
        return insertAlarm(context, alarmDTO, sequenceNumber, peripheralId, session, null);
    }

    public CompletableFuture<AsyncResult<JblEventExtendedJbl>> insertAlarm(JBLContext context, JpsEvtAlarm alarmDTO, JpsSequenceNumber sequenceNumber, String peripheralId, String session, Long jmsOperationId) {
        Logger logger = context.getLogger(this.getClass());
        CompletableFuture<AsyncResult<JblEventExtendedJbl>> promise = new CompletableFuture<>();

        jblTransactionManager.executeTransaction((conn, transactionBodyHandler) -> {

            JblEventExtendedJbl transientEntity = mapper.toEntity(context, alarmDTO);
            transientEntity.setPeripheralId(peripheralId);
            transientEntity.setSequenceNumber(sequenceNumber);
            transientEntity.setSession(session);

            JblAlarm alarmToInsert = ((JblAlarmExtended) transientEntity).getJblAlarm();
            if (jmsOperationId != null) {
                alarmToInsert.setJmsOperationId(jmsOperationId);
            }

            jblAlarmDAO.insert(context, conn, alarmToInsert).thenAccept(jblAlarmAsyncResult -> {
                if (jblAlarmAsyncResult.succeeded()) {
                    logger.info("Event with id: " + jblAlarmAsyncResult.result().getSequenceNumberTs().toString() + " correctly stored");
                    transactionBodyHandler.handle(Future.succeededFuture());

                    transientEntity.setId(jblAlarmAsyncResult.result().getId());
                    transientEntity.setVersion(jblAlarmAsyncResult.result().getVersion());

                    promise.complete(new AsyncResultBuilder<JblEventExtendedJbl>().withSuccess().withResult(transientEntity).build());
                } else if (jblAlarmAsyncResult.cause() instanceof PrimayKeyDuplicatedException) {
                    logger.error("Duplicated sequence found with: " + transientEntity.getSequenceNumber().toString(), jblAlarmAsyncResult.cause());
                    transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                    promise.complete(new AsyncResultBuilder<JblEventExtendedJbl>().withFail().withCause(jblAlarmAsyncResult.cause()).build());
                } else {
                    logger.error("Event storing fails inside dao", jblAlarmAsyncResult.cause());
                    transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                    promise.complete(new AsyncResultBuilder<JblEventExtendedJbl>().withFail().withCause(jblAlarmAsyncResult.cause()).build());
                }
            });


        });

        return promise;
    }

    public CompletableFuture<AsyncResult<JblEventExtendedJbl>> insertEvent(JBLContext context, JpsEvent eventDTO, JpsSequenceNumber sequenceNumber, String peripheralId, String session) {
        Logger logger = context.getLogger(this.getClass());
        CompletableFuture<AsyncResult<JblEventExtendedJbl>> promise = new CompletableFuture<>();

        jblTransactionManager.executeTransaction((conn, transactionBodyHandler) -> {

            JblEventExtendedJbl transientEntity = mapper.toEntity(context, eventDTO);
            transientEntity.setPeripheralId(peripheralId);
            transientEntity.setSequenceNumber(sequenceNumber);
            transientEntity.setSession(session);

            jblEventDAO.insert(context, conn, transientEntity.getJblEvent()).thenAccept(jblAlarmAsyncResult -> {
                if (jblAlarmAsyncResult.succeeded()) {
                    logger.info("Event with id: " + jblAlarmAsyncResult.result().getSequenceNumberTs().toString() + " correctly stored");
                    transactionBodyHandler.handle(Future.succeededFuture());

                    transientEntity.setId(jblAlarmAsyncResult.result().getId());
                    transientEntity.setVersion(jblAlarmAsyncResult.result().getVersion());

                    handleJblEventToContract(transientEntity, jblAlarmAsyncResult);

                    promise.complete(Future.succeededFuture(transientEntity));
                } else if (jblAlarmAsyncResult.cause() instanceof PrimayKeyDuplicatedException) {
                    logger.error("Duplicated sequence found with: " + transientEntity.getSequenceNumber().toString(), jblAlarmAsyncResult.cause());
                    transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                    promise.complete(new AsyncResultBuilder<JblEventExtendedJbl>().withFail().withCause(jblAlarmAsyncResult.cause()).build());
                } else {
                    logger.error("Event storing fails inside dao", jblAlarmAsyncResult.cause());
                    transactionBodyHandler.handle(new AsyncResultBuilder<>().withFail().build());
                    promise.complete(new AsyncResultBuilder<JblEventExtendedJbl>().withFail().withCause(jblAlarmAsyncResult.cause()).build());
                }
            });


        });

        return promise;

    }

    /**
     * get contractCardView in order to populate table "jbl_event_to_contract"
     * @param transientEntity
     * @param jblAlarmAsyncResult
     */
    private void handleJblEventToContract(JblEventExtendedJbl transientEntity, AsyncResult<JblEvent> jblAlarmAsyncResult) {
        if(!(transientEntity.getJpsEvent() instanceof JpsOpUsrTransit))
            return;
        var payload = new JsonObject();
        JpsLogUsrPass usrPass = ((JpsOpUsrTransit<?>) transientEntity.getJpsEvent()).getUsrPass();
        if (usrPass == null)
            return;
        payload.put("jblEventId", jblAlarmAsyncResult.result().getId());
        payload.put("usrPass", SerializationUtils.serialize(usrPass));
        vertx.eventBus().request(JblAPIEventBusBundle.PUB_SUB_QUERY_CONTRACTCARDVIEW_BY_JBLEVENT, payload, responseHandler->gotContractCardView(responseHandler.result()));
    }

    private void gotContractCardView(Message<Object> messageRaw) {
        //var message = (Map)JSONUtil.deserialize(messageRaw.body().toString(), Object.class);
        var message = (JsonObject)messageRaw.body();
        if(message.containsKey("contractNotFound")) {
            return;
        }
        var jblEventId = message.getLong("jblEventId");
        var contractId = message.getLong("contractId");

        //Booking
        if(contractId == null) {
            return;
        }

        var jblEventToContract = new JblEventToContract();
        jblEventToContract.setContractId(contractId);
        jblEventToContract.setJblEventId(jblEventId);
        jblTransactionManager.executeTransaction((conn, transactionFinisher) -> {
            final var context = JBLContext.getInstance();
            jblEventToContractDao.insert(context, conn, jblEventToContract, insertResult -> {
                if(insertResult.failed()) {
                    context.getLogger(this.getClass()).warn("Failure writing jbl_event_to_contract", insertResult.cause());
                } else {
                    //ok
                }
                transactionFinisher.handle(new AsyncResultBuilder().withSuccess().build());
            });
        });
    }

    public CompletableFuture<AsyncResult<Map<String, String>>> jfcShiftId(JBLContext context, JpsAuthenticatedPeripheral jpsAuthenticatedPeripheral) {
        CompletableFuture<AsyncResult<Map<String, String>>> shiftPromise = new CompletableFuture<>();
        HashMap<String, String> extendedSession = Maps.newHashMap();
        if (jpsAuthenticatedPeripheral == null || Strings.isNullOrEmpty(jpsAuthenticatedPeripheral.getType())) {
            shiftPromise.complete(Future.succeededFuture(extendedSession));
        } else {
            if (JpsPeripheral.newInstance(jpsAuthenticatedPeripheral.getType()).isFcj()) {
                new FcjEventService(context, jpsAuthenticatedPeripheral, shiftPromise, extendedSession, jblEventDAO, fcjOptorShiftDao, jblTransactionManager).invoke();
            } else {
                shiftPromise.complete(Future.succeededFuture(extendedSession));
            }
        }
        return shiftPromise;
    }

}
