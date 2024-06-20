package hub.ebb.jblcluster.eventservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.context.JBLContextImpl;
import hub.jbl.common.lib.date.JblDateTime;
import hub.jbl.common.lib.number.MoneyUtils;
import hub.jbl.core.dto.jps.cardValidation.JpsRetCode;
import hub.jbl.core.dto.jps.event.*;
import hub.jbl.core.visitor.jps.JmsCommonEvent;
import hub.jbl.core.visitor.jps.JpsEventMapperVisitor;
import hub.jbl.entity.events.JblEvent;
import hub.jbl.eventservice.generator.InvalidTypeException;
import hub.jbl.eventservice.mapper.JblEventMapperVisitor;
import hub.jbl.eventservice.mapper.JblVisitableEvent;
import hub.jbl.eventservice.mapper.JmsEvent;
import hub.jbl.eventservice.service.MainEventFactory;
import hub.jms.common.model.account.invoice.PostPaymentMethodType;
import hub.jms.common.model.account.product.ProductProfileType;
import hub.jms.common.model.operation.PaymentType;
import hub.jms.common.model.utils.JSONUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JblEventExtendedJbl extends JblEvent implements JblVisitableEvent {

    private JpsSequenceNumber sequenceNumber;
    private JpsEvent jpsEvent;
    private JmsEvent jmsEvent;


    public JblEventExtendedJbl() {

    }

    @JsonIgnore
    public JblEvent getJblEvent() {
        JblEvent jblEvent = new JblEvent();
        jblEvent.setJson(this.getJson());
        jblEvent.setJmsJson(this.getJmsJson());
        jblEvent.setJmsStatus(this.getJmsStatus());
        jblEvent.setSequenceNumberTs(this.getSequenceNumberTs());
        jblEvent.setSequenceNumberGmt(this.getSequenceNumberGmt());
        jblEvent.setSequenceNumberCounter(this.getSequenceNumberCounter());
        jblEvent.setPeripheralId(this.getPeripheralId());
        jblEvent.setEventSpecCode(this.getEventSpecCode());
        jblEvent.setEventType(this.getEventType());
        jblEvent.setId(this.getId());
        jblEvent.setCreationDate(this.getCreationDate());
        jblEvent.setLastModificationDate(this.getLastModificationDate());
        jblEvent.setVersion(this.getVersion());
        jblEvent.setValid(this.getValid());
        jblEvent.setSession(this.getSession());
        jblEvent.setSessionId(this.getSessionId());
        if (this.getJpsEvent() instanceof JpsOpUsrTransit && ((JpsOpUsrTransit<?>) this.getJpsEvent()).getUsrPass() != null) {
            jblEvent.setUsrPassUid(((JpsOpUsrTransit<?>) this.getJpsEvent()).getUsrPass().getUid());
        } else if((this.getJpsEvent() instanceof JpsOpUsrPayCompl && ((JpsOpUsrPayCompl<?>) this.getJpsEvent()).getUsrPass() != null)) {
            jblEvent.setUsrPassUid(((JpsOpUsrPayCompl<?>) this.getJpsEvent()).getUsrPass().getUid());
        }
        return jblEvent;
    }

    private JpsEvent toJson(String json, String eventSpecCode, String eventType) {

        Preconditions.checkNotNull(eventSpecCode);
        Preconditions.checkNotNull(eventType);

        MainEventFactory factoryContainer = MainEventFactory.getInstance();
        JpsEvent prototype = null;
        try {
            prototype = factoryContainer.buildEvent(eventSpecCode, eventType);
        } catch (hub.jbl.core.generator.InvalidTypeException e) {
            throw new RuntimeException(e);
        } catch (InvalidTypeException e) {
            throw new RuntimeException(e);
        }

        return JSONUtil.deserialize(json, prototype.getClass());
    }


    //NOT REMOVE THIS BECAUSE IS USED AT RUNTIME BY ENTITY MAPPER
    public void setSequenceNumberTs(Long ts) {
        if (ts == null) {
            sequenceNumber = null;
            return;
        }

        if (sequenceNumber == null) {
            sequenceNumber = new JpsSequenceNumber();
        }

        if (sequenceNumber.getDateTime() == null) {
            sequenceNumber.setDateTime(new JblDateTime());
        }
        sequenceNumber.setDateTime(sequenceNumber.getDateTime().withTimestamp(ts));
        super.setSequenceNumberTs(ts);
    }

    public Map<String, Object> session2Map() {
        return JBLContextImpl.deserializeJpsSession(getSession());
    }

    //NOT REMOVE THIS BECAUSE IS USED AT RUNTIME BY ENTITY MAPPER
    public void setSequenceNumberGmt(Integer gmt) {

        if (gmt == null) {
            sequenceNumber = null;
            return;
        }

        if (sequenceNumber == null) {
            sequenceNumber = new JpsSequenceNumber();
        }

        if (sequenceNumber.getDateTime() == null) {
            sequenceNumber.setDateTime(new JblDateTime());
        }
        sequenceNumber.setDateTime(sequenceNumber.getDateTime().withGmt(gmt));
        super.setSequenceNumberGmt(gmt);
    }


    //NOT REMOVE THIS BECAUSE IS USED AT RUNTIME BY ENTITY MAPPER
    public void setSequenceNumberCounter(Integer counter) {

        if (counter == null) {
            sequenceNumber = null;
            return;
        }

        if (sequenceNumber == null) {
            sequenceNumber = new JpsSequenceNumber();
        }

        sequenceNumber.setCounter(counter.intValue());
        super.setSequenceNumberCounter(counter.intValue());
    }

    public String getJson() {
        if (this.jpsEvent == null) {
            calculateJpsEvent();
        }
        return JSONUtil.serialize(this.jpsEvent);
    }


    public String getJmsJson() {
        if (this.jmsEvent == null) {
            calculateJmsEvent();
        }
        return JSONUtil.serialize(this.jmsEvent);
    }


    @JsonIgnore
    public JpsEvent getJpsEvent() {
        if (jpsEvent == null) {
            calculateJpsEvent();
        }
        return jpsEvent;
    }

    @JsonIgnore
    public void setJpsEvent(JpsEvent jpsEvent) {
        setEventSpecCode(jpsEvent.getEventSpecCode().getValue());
        setEventType(jpsEvent.getEventType());
        this.jpsEvent = jpsEvent;
        setJson(JSONUtil.serialize(jpsEvent));
        setSessionId(jpsEvent.getSessionId());
    }


    public void setJmsStatus(JmsStatus jmsStatus) {
        super.setJmsStatus(jmsStatus==null?null:jmsStatus.name());
    }

    public JpsSequenceNumber getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(JpsSequenceNumber jpsSequenceNumber) {
        if (jpsSequenceNumber != null) {
            super.setSequenceNumberCounter(jpsSequenceNumber.getCounter());
            super.setSequenceNumberGmt(jpsSequenceNumber.getDateTime().getGmt());
            super.setSequenceNumberTs(jpsSequenceNumber.getDateTime().getTimestamp());
        } else {
            super.setSequenceNumberCounter(null);
            super.setSequenceNumberGmt(null);
            super.setSequenceNumberTs(null);
        }
        this.sequenceNumber = jpsSequenceNumber;
    }

    public String eventBusPublishUrl() {

        if (jpsEvent != null) {
            return jpsEvent.eventBusPublishUrl();
        }
        return null;
    }

    public void calculateJpsEvent() {
        long start = System.currentTimeMillis();
        if (!StringUtils.isEmpty(getEventSpecCode()) && !StringUtils.isEmpty(getEventType()) && !StringUtils.isEmpty(this.json)) {
            this.jpsEvent = toJson(this.json, getEventSpecCode(), getEventType());
        }
        JBLContext.getInstance().getLogger(this.getClass()).debug("CALCULATE JPS EVENT elapsed time: " + (System.currentTimeMillis() - start) + " ms");
    }

    public void calculateJmsEvent() {
        if (!StringUtils.isEmpty(this.jmsEvent)) {
            this.jmsEvent = JSONUtil.deserialize(getJmsJson(), JmsEvent.class);
        }
    }

    public JmsEvent getJmsEvent() {
        if (jmsEvent == null)
            calculateJmsEvent();
        return jmsEvent;
    }

    public void setJmsEvent(JmsEvent jmsEvent) {
        this.jmsEvent = jmsEvent;
    }

    @Override
    public CompletableFuture<AsyncResult<JmsEvent>> accept(JblEventMapperVisitor visitor) {

        CompletableFuture<AsyncResult<JmsEvent>> promise = new CompletableFuture<>();
        getJpsEvent().accept(visitor).thenAccept(jmsCommonEventAsyncResult -> {
            JmsEvent jmsEvent = null;
            if (jmsCommonEventAsyncResult.result() != null) {
                jmsEvent = (JmsEvent) jmsCommonEventAsyncResult.result();
            }
            if (jmsCommonEventAsyncResult.succeeded()) {
                promise.complete(Future.succeededFuture(jmsEvent));
            } else {
                promise.complete(Future.failedFuture(jmsCommonEventAsyncResult.cause()));
            }
        });

        return promise;

    }

    @Override
    public CompletableFuture<AsyncResult<JmsCommonEvent>> accept(JpsEventMapperVisitor visitor) {
        CompletableFuture<AsyncResult<JmsCommonEvent>> promise = new CompletableFuture<>();
        try {
            ((JblEventMapperVisitor) visitor).map(this).thenAccept(jmsEventAsyncResult -> {
                promise.complete(Future.succeededFuture(jmsEventAsyncResult.result()));
            });
        } catch (Exception e) {
            hub.jbl.common.lib.context.JBLContext.getInstance().getLogger(getClass()).error("Error on mapping " + getClass().getName() + " event.", e);
            promise.complete(Future.failedFuture(e));
        }
        return promise;

    }

    public static class Builder<E extends JblEventExtendedJbl, B extends Builder> {
        protected E instance;

        private long ts;
        private int gmt;
        private long counter;

        public Builder(String eventSpecCode, String eventType) {

            instance = createInstance();
            instance.setEventSpecCode(eventSpecCode);
            instance.setEventType(eventType);
            MainEventFactory factoryContainer = MainEventFactory.getInstance();
            JpsEvent jpsEvent = null;
            try {
                jpsEvent = factoryContainer.buildEvent(eventSpecCode, eventType);
            } catch (InvalidTypeException e) {
                e.printStackTrace();
            } catch (hub.jbl.core.generator.InvalidTypeException e) {
                e.printStackTrace();
            }

            jpsEvent.setEventSpecCode(SpecCodeEnum.forValue(eventSpecCode));
            jpsEvent.setEventType(eventType);
            getInstance().setJpsEvent(jpsEvent);
        }

        public B withSequenceNumberTS(long ts) {
            this.ts = ts;
            return (B) this;
        }

        public B withSequenceNumberGMT(int gmt) {
            this.gmt = gmt;
            return (B) this;
        }

        public B withSequenceNumberCounter(long counter) {
            this.counter = counter;
            return (B) this;
        }

        public B withSessionId(Long sessionId) {
            getInstance().getJpsEvent().setSessionId(sessionId);
            getInstance().setSessionId(sessionId);
            return (B) this;
        }

        public B withPeripheralId(String peripheralId) {
            getInstance().setPeripheralId(peripheralId);
            return (B) this;
        }

        public B withPeripheralType(String peripheralType) {
            getInstance().getJpsEvent().setPeripheralType(peripheralType);
            return (B) this;
        }

        public B withSeverity(JpsEventSeverity eventSeverity) {
            getInstance().getJpsEvent().setSeverity(eventSeverity);
            return (B) this;
        }

        public B withJmsStatus(JmsStatus jmsStatus) {
            getInstance().setJmsStatus(jmsStatus);
            return (B) this;
        }

        public B withEntityCode(String entityCode) {
            getInstance().getJpsEvent().setEntityCode(entityCode);
            return (B) this;
        }

        public E build() {
            instance.setSequenceNumber(new JpsSequenceNumber(ts, gmt, new Long(counter).intValue()));
            instance.setJson(JSONUtil.serialize(instance.getJpsEvent()));
            return instance;
        }

        protected E createInstance() {
            return (E) new JblEventExtendedJbl();
        }

        protected E getInstance() {
            return instance;
        }
    }

    public static class UsrPassBuilder extends Builder<JblEventExtendedJbl, UsrPassBuilder> {

        public UsrPassBuilder(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
        }

        public UsrPassBuilder isReserved(boolean reserved) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setReserved(reserved);
            return this;
        }

        public UsrPassBuilder withContext(JpsUsrPassContext context) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setContext(context);
            return this;
        }

        public UsrPassBuilder withType(JpsUsrPassType usrPassType) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setUsrType(usrPassType);
            return this;
        }

        public UsrPassBuilder withProductProfileType(hub.jbl.core.dto.jps.event.ProductProfileType productProfileType) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setPrdType(productProfileType);
            return this;
        }

        public UsrPassBuilder withLicensePlate(String licensePlate) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setLicensePlate(licensePlate);
            return this;
        }

        public UsrPassBuilder withMediaType(JpsUsrPassMediaType mediaType) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setMediaType(mediaType);
            return this;
        }

        public UsrPassBuilder withUid(String uid) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setUid(uid);
            return this;
        }

        public UsrPassBuilder withNowTs(long ts) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setNowTs(ts);
            return this;
        }

        public UsrPassBuilder withVehicleCategory(JpsLogVcsStatus vehicleCategory) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setVehicleCategory(vehicleCategory);
            return this;
        }


        public UsrPassBuilder withNowGmt(int gmt) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setNowGmt(gmt);
            return this;
        }

        public UsrPassBuilder withRawData(String rowData) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setRawData(rowData);
            return this;
        }

        public UsrPassBuilder withSourceCode(String sourceCode) {
            JblEventExtendedJbl log = super.build();
            JpsLogUsrPass usrPass = (JpsLogUsrPass) log.getJpsEvent();
            usrPass.setVehicleCategory(JpsLogVcsStatus.forValue(Long.parseLong(sourceCode)));
            return this;
        }

        @Override
        protected JblEventExtendedJbl createInstance() {
            return new JblUsrPass();
        }
    }

    public static class TransitOperationBuilder extends Builder<JblEventExtendedJbl, TransitOperationBuilder> {

//        private JpsLogVcsStatus vehicleCategory;
//        private JpsTransitType transitType;

        public TransitOperationBuilder(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
        }

        public TransitOperationBuilder withVehicleCategory(JpsLogVcsStatus vsc) {
            JblEventExtendedJbl log = super.build();
            JpsOpUsrTransit transit = (JpsOpUsrTransit) log.getJpsEvent();
            transit.setVehicleCategory(vsc);
            return this;
        }

        public TransitOperationBuilder withSession(JpsRetCode retCode) {
            JblEventExtendedJbl log = super.build();
            JBLContext jblContext = new JBLContextImpl();
            jblContext.addSessionField("retCode", retCode.name());
            log.setSession(jblContext.serializeJpsSession());
            return this;
        }

        public TransitOperationBuilder withTransitType(JpsTransitType transitType) {
            JblEventExtendedJbl log = super.build();
            JpsOpUsrTransit transit = (JpsOpUsrTransit) log.getJpsEvent();
            transit.setTransitType(transitType);
            return this;
        }

        public TransitOperationBuilder withDeductedBalance(Double balance) {
            JblEventExtendedJbl log = super.build();
            JpsOpUsrTransit transit = (JpsOpUsrTransit) log.getJpsEvent();
            transit.setDeductedAmount(MoneyUtils.getMoney(balance).doubleValue());
            return this;
        }


        public TransitOperationBuilder withUsrPass(JpsLogUsrPass usrPass) {
            JblEventExtendedJbl log = super.build();
            JpsOpUsrTransit transit = (JpsOpUsrTransit) log.getJpsEvent();
            transit.setUsrPass(usrPass);
            return this;
        }
    }

    public static class CardValidationOperationBuilder extends Builder<JblEventExtendedJbl, CardValidationOperationBuilder> {


        public CardValidationOperationBuilder() {
            super("JpsUsrCardValidationResult", "0x00001e80");
        }


        public CardValidationOperationBuilder withDeductedAmount(Double value) {
            JblEventExtendedJbl log = super.build();
            JpsUsrCardValidationResult cardValidation = (JpsUsrCardValidationResult) log.getJpsEvent();
            cardValidation.setDeductedAmount(MoneyUtils.getMoney(value));
            return this;
        }

//        public CardValidationOperationBuilder withRetCode(JpsRetCode retCode) {
//            JblEvent log = super.build();
//            JpsUsrCardValidationResult cardValidation = (JpsUsrCardValidationResult) log.getJpsEvent();
//            cardValidation.setRetCode( retCode );
//            return this;
//        }

        public CardValidationOperationBuilder withAmount(Double amt) {
            JblEventExtendedJbl log = super.build();
            JpsUsrCardValidationResult cardValidation = (JpsUsrCardValidationResult) log.getJpsEvent();
            cardValidation.setAmount(MoneyUtils.getMoney(amt));
            return this;
        }

//        public CardValidationOperationBuilder withContractNumber(String contractNumber) {
//            JblEvent log = super.build();
//            JpsUsrCardValidationResult cardValidation = (JpsUsrCardValidationResult) log.getJpsEvent();
//            cardValidation.setContractNumber( contractNumber );
//            return this;
//        }

        public CardValidationOperationBuilder withType(JpsUsrPassType usrPassType) {
            JblEventExtendedJbl log = super.build();
            JpsUsrCardValidationResult cardValidation = (JpsUsrCardValidationResult) log.getJpsEvent();
            cardValidation.setType(usrPassType);
            return this;
        }

        public CardValidationOperationBuilder withProductProfileType(ProductProfileType prdType) {
            JblEventExtendedJbl log = super.build();
            JpsUsrCardValidationResult cardValidation = (JpsUsrCardValidationResult) log.getJpsEvent();
            cardValidation.setProdProfileType(prdType.name());
            return this;
        }

        public CardValidationOperationBuilder withUsrPass(JpsLogUsrPass usrPass) {
            JblEventExtendedJbl log = super.build();
            JpsUsrCardValidationResult transit = (JpsUsrCardValidationResult) log.getJpsEvent();
            transit.setUsrPass(usrPass);
            return this;
        }
    }

    public static class LogBuilder extends Builder<JblEventExtendedJbl, LogBuilder> {

        public LogBuilder(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
        }

        public JblEventExtendedJbl buildReboot(JpsRebootMode mode) {
            JblEventExtendedJbl log = super.build();
            JpsLogReboot reboot = (JpsLogReboot) log.getJpsEvent();
            reboot.setMode(mode);

            log.setJpsEvent(reboot);

            return log;
        }

        public JblEventExtendedJbl buildLaneLoopLog(JpsLogLoopType type, JpsLogLoopStatus status) {
            JblEventExtendedJbl log = super.build();
            JpsLogLaneLoopAct loop = (JpsLogLaneLoopAct) log.getJpsEvent();
            loop.setType(type);
            loop.setStatus(status);
            log.setJpsEvent(loop);

            return log;
        }

        public JblEventExtendedJbl buildLaneBarrierLog(JpsLogBarType type, JpsLogBarStatus status) {
            JblEventExtendedJbl log = super.build();
            JpsLogLaneBarAct barrier = (JpsLogLaneBarAct) log.getJpsEvent();
            barrier.setType(type);
            barrier.setStatus(status);

            log.setJpsEvent(barrier);

            return log;
        }

        public JblEventExtendedJbl buildLaneButtonLog(JpsLogButtonType type, JpsLogButtonStatus status) {
            JblEventExtendedJbl log = super.build();
            JpsLogLaneButtonAct button = (JpsLogLaneButtonAct) log.getJpsEvent();
            button.setType(type);
            button.setStatus(status);

            log.setJpsEvent(button);

            return log;
        }

        public JblEventExtendedJbl buildLaneVcsLog(JpsLogVcsType type, JpsLogVcsStatus status) {
            JblEventExtendedJbl log = super.build();
            JpsLogLaneVCSAct vcs = (JpsLogLaneVCSAct) log.getJpsEvent();
            vcs.setType(type);
            vcs.setStatus(status);

            log.setJpsEvent(vcs);

            return log;
        }
    }

    public static class ExternalPaymentResultOperation extends Builder<JblEventExtendedJbl, ExternalPaymentResultOperation> {

        public ExternalPaymentResultOperation(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
            JmsExternalPaymentComplete jmsExternalPaymentComplete = (JmsExternalPaymentComplete) getInstance().getJpsEvent();
            jmsExternalPaymentComplete.setData(new JpsPaymentData());
        }

        public ExternalPaymentResultOperation withCurrency(String currency) {
            JmsExternalPaymentComplete jmsExternalPaymentComplete = (JmsExternalPaymentComplete) getInstance().getJpsEvent();
            jmsExternalPaymentComplete.setCurrency(currency);
            return this;
        }

        public ExternalPaymentResultOperation withUsrPass(JpsLogUsrPass pass) {
            JmsExternalPaymentComplete jmsExternalPaymentComplete = (JmsExternalPaymentComplete) getInstance().getJpsEvent();
            jmsExternalPaymentComplete.setUsrPass(pass);
            return this;
        }

        public ExternalPaymentResultOperation withAmounts(BigDecimal reqAmount, BigDecimal acqAmount, BigDecimal cngAmount, Double vatAmount) {
            JmsExternalPaymentComplete jmsExternalPaymentComplete = (JmsExternalPaymentComplete) getInstance().getJpsEvent();
            jmsExternalPaymentComplete.setReqAmount(reqAmount);
            jmsExternalPaymentComplete.getData().setAcqAmount(acqAmount);
            jmsExternalPaymentComplete.getData().setCngAmount(cngAmount);
            jmsExternalPaymentComplete.getData().setError(JpsPaymentError.ErrNone);
            if (vatAmount != null) {
                jmsExternalPaymentComplete.getData().setVatAmount(vatAmount);
            }
            return this;
        }

        public ExternalPaymentResultOperation withPaymentUniqueReferenceId(String paymentUniqueReferenceId) {
            JmsExternalPaymentComplete jmsExternalPaymentComplete = (JmsExternalPaymentComplete) getInstance().getJpsEvent();
            jmsExternalPaymentComplete.setPaymentUniqueReferenceId(paymentUniqueReferenceId);
            return this;
        }

        public ExternalPaymentResultOperation withPaymentType(PaymentType paymentType) {
            if (paymentType != null) {
                JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
                opPayCompl.getData().setPaymentType(paymentType.name());
            }
            return this;
        }

    }

    public static class PaymentResultOperation extends Builder<JblEventExtendedJbl, PaymentResultOperation> {

        public PaymentResultOperation(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.setData(new JpsPaymentData());
        }

        public PaymentResultOperation withCurrency(String currency) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.setCurrency(currency);
            return this;
        }

        public PaymentResultOperation withError(JpsPaymentError error) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.getData().setError(error);
            return this;
        }

        public PaymentResultOperation withPaymentType(PaymentType paymentType) {
            if (paymentType != null) {
                JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
                opPayCompl.getData().setPaymentType(paymentType.name());
            }
            return this;
        }

        public PaymentResultOperation withAmounts(BigDecimal reqAmount, BigDecimal acqAmount, BigDecimal cngAmount) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.setReqAmount(reqAmount);
            opPayCompl.getData().setAcqAmount(acqAmount);
            opPayCompl.getData().setCngAmount(cngAmount);
            opPayCompl.getData().setError(JpsPaymentError.ErrNone);
            return this;
        }

        /**
         * For Points Card product profiles, set the amount of points recharged on the JpsEvent.
         *
         * @param pointsRecharged The amount of points being recharged with the event.  This is truncated from a BigDecimal to and Integer.
         * @return
         */
        public PaymentResultOperation withPointsRecharge(BigDecimal pointsRecharged) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            if (opPayCompl.getUsrPass().getPrdType() == hub.jbl.core.dto.jps.event.ProductProfileType.POINTS_CARD) {
                opPayCompl.setPointsRecharged(pointsRecharged.intValue());
            }

            opPayCompl.getData().setError(JpsPaymentError.ErrNone);
            return this;
        }

        public PaymentResultOperation withUsrPass(JpsLogUsrPass pass) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.setUsrPass(pass);
            return this;
        }

        public PaymentResultOperation withoutCacheData() {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.getData().setCashData(null);
            return this;
        }

        public PaymentResultOperation withCacheData(BigDecimal amount, long paydifftime, JpsCashDenomination... denoms) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            JpsCashData data = new JpsCashData();
            data.setAmount(amount);
            data.setDenoms(denoms);
            opPayCompl.getData().setCashData(data);
            opPayCompl.getData().setPaydifftime(paydifftime);
            return this;
        }

        public PaymentResultOperation withCacheData(BigDecimal amount, JpsCashDenomination... denoms) {
            withCacheData(amount, 0, denoms);
            return this;
        }

        public PaymentResultOperation withPosData(BigDecimal amount) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            JpsPosData data = new JpsPosData();
            data.setAmount(amount);
            data.setCcdata(new JpsCCCardData());
            data.getCcdata().setMaskedPan("123");
            data.getCcdata().setTransactionToken("ttt");
            data.getCcdata().setExpiryDate("1218");
            data.getCcdata().setCardBrand("VISA");
            opPayCompl.getData().setPosData(data);
            return this;
        }

        public PaymentResultOperation withPaymentUniqueReferenceId(String paymentUniqueReferenceId) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.setPaymentUniqueReferenceId(paymentUniqueReferenceId);
            return this;
        }

        public PaymentResultOperation withPaymentDataSubtype(JblPaymentDataSubtype subtype) {
            JpsOpUsrPayCompl opPayCompl = (JpsOpUsrPayCompl) getInstance().getJpsEvent();
            opPayCompl.getData().setSubtype(subtype);
            return this;
        }

        public PaymentResultOperation withDescription(String description) {
            getInstance().getJpsEvent().setDescription(description);
            return this;
        }

    }

    public static class ChangeGvnOperation extends Builder<JblEventExtendedJbl, ChangeGvnOperation> {

        public ChangeGvnOperation(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
            JpsOpUsrChangeGvn change = (JpsOpUsrChangeGvn) getInstance().getJpsEvent();
            change.setData(new JpsOpChangeGivenData());
        }

        public ChangeGvnOperation withCurrency(String currency) {
            JpsOpUsrChangeGvn change = (JpsOpUsrChangeGvn) getInstance().getJpsEvent();
            change.setCurrency(currency);
            return this;
        }

        public ChangeGvnOperation withAmounts(BigDecimal reqAmount, BigDecimal cngAmount) {
            JpsOpUsrChangeGvn change = (JpsOpUsrChangeGvn) getInstance().getJpsEvent();
            change.setReqAmount(reqAmount);
            change.getData().setAmount(cngAmount);
            change.getData().setDenoms(new JpsCashDenomination[0]);
            return this;
        }

        public ChangeGvnOperation withUsrPass(JpsLogUsrPass pass) {
            JpsOpUsrChangeGvn change = (JpsOpUsrChangeGvn) getInstance().getJpsEvent();
            change.setUsrPass(pass);
            return this;
        }

        public ChangeGvnOperation withContext(JpsUsrPassContext context) {
            JpsOpUsrChangeGvn change = (JpsOpUsrChangeGvn) getInstance().getJpsEvent();
            change.setContext(context);
            return this;
        }

        public ChangeGvnOperation withDescription(String description) {
            getInstance().getJpsEvent().setDescription(description);
            return this;
        }
    }

    public static class BillingOperation extends Builder<JblEventExtendedJbl, BillingOperation> {

        public BillingOperation(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
        }

        public BillingOperation withInvoiceData(long invoiceId, BigDecimal amount, long fromDate, long toDate,
                                                String contractNumber, String cardNumber, String notes, hub.jbl.core.dto.jps.event.ProductProfileType productProfileType) {
            JmsBillingOperation jmsBillingOperation = (JmsBillingOperation) getInstance().getJpsEvent();
            jmsBillingOperation.setInvoiceId(invoiceId);
            jmsBillingOperation.setAmount(amount);
            jmsBillingOperation.setFromDate(fromDate);
            jmsBillingOperation.setToDate(toDate);
            jmsBillingOperation.setContractNumber(contractNumber);
            jmsBillingOperation.setCardNumber(cardNumber);
            jmsBillingOperation.setNotes(notes);
            jmsBillingOperation.setProductProfileType(productProfileType);
            return this;
        }
    }

    public static class PaymentOperation extends Builder<JblEventExtendedJbl, PaymentOperation> {

        public PaymentOperation(String eventSpecCode, String eventType) {
            super(eventSpecCode, eventType);
        }

        public PaymentOperation withPaymentData(long invoiceId, PostPaymentMethodType type, String notes, String payer,
                                                String cardIdentifier, BigDecimal amount, Double vatAmount, String contractIdentifier, String entityId, long paymentId,
                                                long periodFrom, Short periodFromGmt, long periodTo, Short periodToGmt) {
            JmsPaymentOperation jmsPaymentOperation = (JmsPaymentOperation) getInstance().getJpsEvent();
            jmsPaymentOperation.setInvoiceId(invoiceId);
            jmsPaymentOperation.setType(type);
            jmsPaymentOperation.setNotes(notes);
            jmsPaymentOperation.setPayer(payer);
            jmsPaymentOperation.setCardIdentifier(cardIdentifier);
            jmsPaymentOperation.setAmount(amount);
            jmsPaymentOperation.setContractIdentifier(contractIdentifier);
            jmsPaymentOperation.setEntityId(entityId);
            jmsPaymentOperation.setPaymentId(paymentId);
            jmsPaymentOperation.setPeriodFrom(periodFrom);
            jmsPaymentOperation.setPeriodFromGmt(periodFromGmt);
            jmsPaymentOperation.setPeriodTo(periodTo);
            jmsPaymentOperation.setPeriodToGmt(periodToGmt);

            JpsPaymentData jpsPaymentData = new JpsPaymentData();
            jpsPaymentData.setError(JpsPaymentError.ErrNone);
            jpsPaymentData.setAcqAmount(amount);
            jpsPaymentData.setCngAmount(BigDecimal.ZERO);
            jpsPaymentData.setSubtype(JblPaymentDataSubtype.JBL_PAYMENT_DATA_SUBTYPE_NONE);
            jpsPaymentData.setVatAmount(vatAmount);

            jmsPaymentOperation.setData(jpsPaymentData);
            return this;
        }
    }
}
