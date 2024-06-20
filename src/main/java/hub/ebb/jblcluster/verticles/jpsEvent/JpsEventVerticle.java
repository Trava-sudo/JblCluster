package hub.ebb.jblcluster.verticles.jpsEvent;

import hub.jbl.common.JblConfig;
import hub.jbl.common.lib.JblAPIEventBusBundle;
import hub.jbl.common.lib.R;
import hub.jbl.common.lib.SessionFields;
import hub.jbl.common.lib.api.counters.CountersAPI;
import hub.jbl.common.lib.api.event.EventAPI;
import hub.jbl.common.lib.api.event.dto.JpsEventDto;
import hub.jbl.common.lib.api.peripheral.JpsAuthenticatedPeripheralAPI;
import hub.jbl.common.lib.builder.AsyncResultBuilder;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.date.DateUtils;
import hub.jbl.common.lib.log.Logger;
import hub.jbl.common.lib.utils.HttpServerUtils;
import hub.jbl.common.lib.utils.serviceDiscovery.IServiceDiscoveryClient;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.common.session.RemoteDiscountSessionData;
import hub.jbl.common.verticles.AbstractRestVerticle;
import hub.jbl.core.dto.jps.event.*;
import hub.jbl.dao.JblConfigDao;
import hub.jbl.dao.JblEventDao;
import hub.jbl.common.dao.authentication.JpsAuthenticatedPeripheral;
import hub.jbl.entity.events.JblEvent;
import hub.jbl.entity.fiscal.FiscalPolicyType;
import hub.jbl.entity.fiscal.FiscalPrinterStatusType;
import hub.jbl.entity.jpsCommand.request.JpsCommandRequest;
import hub.jbl.entity.jpsCommand.request.JpsCommandRequestType;
import hub.jbl.entity.jpsCommand.request.peripheral.JpsGetFiscalPrinterConfigurationCommand;
import hub.jbl.entity.jpsCommand.response.JpsCommandResponse;
import hub.jbl.entity.parknode.PeripheralStatusValues;
import hub.jbl.entity.peripheralStatus.peripheral.JpsPeripheralStatus;
import hub.ebb.jblcluster.eventservice.model.JblAlarmExtended;
import hub.ebb.jblcluster.eventservice.model.JblEventExtendedJbl;
import hub.ebb.jblcluster.eventservice.model.JmsStatus;
import hub.ebb.jblcluster.eventservice.model.JpsSequenceNumber;
import hub.ebb.jblcluster.eventservice.model.bundle.JblEventBundle;
import hub.ebb.jblcluster.eventservice.model.factory.InvalidJpsEventTypeException;
import hub.ebb.jblcluster.eventservice.service.EventSequenceNumberGenerator;
import hub.ebb.jblcluster.eventservice.service.JblCounterSourceService;
import hub.ebb.jblcluster.eventservice.service.JpsEventService;
import hub.ebb.jblcluster.eventservice.service.MainEventFactory;
import hub.jbl.core.dto.jps.authentication.common.JpsPeripheral;
//import hub.jbl.services.authentication.AuthenticationService;
import hub.jms.common.model.configuration.JblFiscalPrinterConfiguration;
import hub.jms.common.model.utils.JSONUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Created by Stefano.Coletta on 08/11/2016.
 */
public class JpsEventVerticle extends AbstractRestVerticle implements JpsEventVerticleEndPoint, EventAPI {

    public static String JPS_EVENT_MICROSERVICE_PATH = "\\/jbl\\/api\\/peripherals\\/.*\\/event\\/.*\\/.*\\/.*";

    public static String JPS_EVENT_ENDPOINT_PATH = "/jbl/api/peripherals/:peripheralId/event/:sequenceNumberTS/:sequenceNumberGMT/:sequenceNumberCounter";

    @Autowired
    protected JpsEventService jpsEventService;
    @Autowired
    JblCounterSourceService jblCounterSourceService;
    @Autowired
    JblTransactionManager jblTransactionManager;

    @Autowired
    private JblEventDao jblEventDao;

    @Autowired
    JblConfigDao jblConfigDao;
    MessageConsumer<String> mc1;
    MessageConsumer<String> mc2;
    MessageConsumer<String> mc3;
    @Autowired
    private IServiceDiscoveryClient serviceDiscoveryClient;
//    @Autowired
//    private AuthenticationService authenticationService;
    @Autowired
    @Qualifier("jpsAuthenticatedPeripheralAPI")
    private JpsAuthenticatedPeripheralAPI authenticatedPeripheralAPI;

    @Autowired
    private RemoteDiscountSessionData remoteDiscountSessionData;

    private Map<String, JpsAuthenticatedPeripheral> authenticatedPeripheralCache;
    private Map<String, String> deviceStatuses;


    @Override
    protected boolean isSwaggerImplemented() {
        return true;
    }

    //---------------------------------ACTOR SETUP--------------------------------
    @Override
    protected void handleRestEndpoint(Router router) {

        router.post(getServiceEndpoint()).handler(this::doEventManagement);
    }

    @Override
    protected String getServiceEndpoint() {
        return JPS_EVENT_ENDPOINT_PATH;
    }

    @Override
    protected String getRegExpMicroServiceName() {
        return JPS_EVENT_MICROSERVICE_PATH;
    }

    @Override
    protected void registerProxy(ServiceBinder serviceBinder) {
        serviceBinder.setAddress(EventAPI.ADDRESS).register(EventAPI.class, this);
        serviceBinder.setAddress(R.PROXY_API_COUNTERS).register(CountersAPI.class, jblCounterSourceService);
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {

        context().getLogger(this.getClass()).info("Starting JpsEvent Verticle..");
        authenticatedPeripheralCache = new HashMap<>();
        vertx.eventBus().consumer(JblAPIEventBusBundle.JBL_ALARM_SENDER_CLOCK_DISALIGNMENT, this::sendAlarmForClockAlignment);
        vertx.eventBus().consumer(JblAPIEventBusBundle.JBL_ALARM_SENDER_GATELESS_STATUS, this::sendAlarmForGatelessDeviceStatus);
        vertx.eventBus().consumer(JblAPIEventBusBundle.JBL_ALARM_SENDER_JPS_OFFLINE, this::sendAlarmForPeripheralOffline);

        deviceStatuses = new HashMap<>();

        jblTransactionManager.executeTransaction((conn, transactionBodyResultHandler) -> {
            jblConfigDao.getSetting(context(), conn, JblConfig.JBL_CONFIG_FISCALPOLICY, event -> {
                if (event.succeeded()) {
                    transactionBodyResultHandler.handle(Future.succeededFuture());

//String fiscalPolicy = FiscalPolicyType.getEnum(event.result().toUpperCase()).name();
                    String fiscalPolicy = FiscalPolicyType.mapToString(event.result().toUpperCase());

                    config().remove(JblConfig.JBL_CONFIG_FISCALPOLICY);
                    config().put(JblConfig.JBL_CONFIG_FISCALPOLICY, fiscalPolicy);

                    if (fiscalPolicy.equals(FiscalPolicyType.BULGARIA.name())) {
                        registerFiscalPrinterStatusConsumers();

                        vertx.setTimer(30000, handler -> {
                            initFiscalPrinterStatuses();
                        });
                    }
                } else {
                    transactionBodyResultHandler.handle(new AsyncResultBuilder<>().withFail().build());

                    config().remove(JblConfig.JBL_CONFIG_FISCALPOLICY);
                    config().put(JblConfig.JBL_CONFIG_FISCALPOLICY, FiscalPolicyType.NONE.name());

                    context().getLogger(this.getClass()).warn("No setting for fiscalPolicy found");
                }
            });
        });

        vertx.eventBus().consumer(JblAPIEventBusBundle.PUB_UPDATE_NODE_SETTINGS, this::onEbbSystemParametersChange);

        vertx.eventBus().consumer(JblAPIEventBusBundle.CLEAN_AUTHENTICATION_EVENT_PRODUCER_URL, message -> {
            jpsEventService.changePrinterStatus(context(), message.body().toString(), FiscalPrinterStatusType.Unavailable);
        });

        vertx.eventBus().consumer(JblAPIEventBusBundle.REQUEST_UPDATED_INSERTED_NODE, jblCounterSourceService::onParkingNodeUpdate);

        jblCounterSourceService.initializeService(resultHandler -> {
            try {
                super.start(promise);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void onEbbSystemParametersChange(Message<JsonObject> settings) {
        String fiscalPolicy = settings.body().getJsonObject("application").getString(JblConfig.JBL_CONFIG_FISCALPOLICY, FiscalPolicyType.NONE.name());

        config().remove(JblConfig.JBL_CONFIG_FISCALPOLICY);
        config().put(JblConfig.JBL_CONFIG_FISCALPOLICY, fiscalPolicy);

        if (fiscalPolicy.equalsIgnoreCase(FiscalPolicyType.BULGARIA.name())) {
            initFiscalPrinterStatuses();
            registerFiscalPrinterStatusConsumers();
        } else {
            unregisterFiscalPrinterStatusConsumers();
        }
    }

    private void registerFiscalPrinterStatusConsumers() {
        mc1 = vertx.eventBus().consumer(JblAPIEventBusBundle.REQUEST_RESPONSE_EVENT_REBOOT_PERIPHERAL_3, this::handleReboot);
        mc2 = vertx.eventBus().consumer(hub.jbl.common.lib.JblAPIEventBusBundle.PUB_OUT_OF_ORDER, this::handleOOA);
        mc3 = vertx.eventBus().consumer(JblAPIEventBusBundle.SEND_PERIPHERAL_STATUS_URL, this::handleChangePeripheralStatus);
    }

    private void unregisterFiscalPrinterStatusConsumers() {
        if (mc1 != null && mc1.isRegistered()) mc1.unregister();
        if (mc2 != null && mc2.isRegistered()) mc2.unregister();
        if (mc3 != null && mc3.isRegistered()) mc3.unregister();
    }

    private void initFiscalPrinterStatuses() {
//        authenticationService.getAllAuthenticatedPeripherals(context()).thenAccept(listAsyncResult -> {
//            if (listAsyncResult.succeeded()) {
//                for (JpsAuthenticatedPeripheral peripheral : listAsyncResult.result()) {
//                    getFiscalPrinterConfiguration(peripheral.getPeripheralId(), fiscalPrinterConfigurationAsyncResult -> {
//                        if (fiscalPrinterConfigurationAsyncResult.succeeded()) {
//                            jpsEventService.changeOrCreatePrinterStatus(context(), peripheral.getPeripheralId(), fiscalPrinterConfigurationAsyncResult.result());
//                        }
//                    });
//                }
//            }
//        });
    }

    private void handleChangePeripheralStatus(Message<String> message) {
        JpsPeripheralStatus status = JSONUtil.deserialize(message.body(), JpsPeripheralStatus.class);
        boolean update = false;
        if (deviceStatuses.containsKey(status.getPeripheralId())) {
            if (!deviceStatuses.get(status.getPeripheralId()).equalsIgnoreCase(status.getStatus())) {
                deviceStatuses.put(status.getPeripheralId(), status.getStatus());
                update = true;
            }
        } else {
            deviceStatuses.put(status.getPeripheralId(), status.getStatus());
            update = true;
        }

        if (update) {
            context().getLogger(getClass()).info("EBBS-1170: Device " + status.getPeripheralId() + " changed status to " + status.getStatus());
            if (status.getStatus().equalsIgnoreCase(PeripheralStatusValues.Offline.name()) || status.getStatus().equalsIgnoreCase(PeripheralStatusValues.OutOfService.name())
                    || status.getStatus().equalsIgnoreCase(PeripheralStatusValues.OutOfOrder.name())) {
                jpsEventService.changePrinterStatus(context(), status.getPeripheralId(), FiscalPrinterStatusType.Unavailable);
            } else {
                getFiscalPrinterConfiguration(status.getPeripheralId(), event -> {
                    if (event.succeeded()) {
                        jpsEventService.changeOrCreatePrinterStatus(context(), status.getPeripheralId(), event.result());
                    } else {
                        jpsEventService.changePrinterStatus(context(), status.getPeripheralId(), FiscalPrinterStatusType.Unavailable);
                    }
                });
            }
        }

    }

    private void handleReboot(Message<String> message) {
        JblEventExtendedJbl jblEvent = JSONUtil.deserialize(message.body(), JblEventExtendedJbl.class);
        context().getLogger(getClass()).info("EBBS-1170: Device " + jblEvent.getPeripheralId() + " rebooted");
        getFiscalPrinterConfiguration(jblEvent.getPeripheralId(), event -> {
            if (event.succeeded()) {
                jpsEventService.changeOrCreatePrinterStatus(context(), jblEvent.getPeripheralId(), event.result());
            } else {
                jpsEventService.changePrinterStatus(context(), jblEvent.getPeripheralId(), FiscalPrinterStatusType.Unavailable);
            }
        });
    }

    private void getFiscalPrinterConfiguration(String peripheralId, Handler<AsyncResult<JblFiscalPrinterConfiguration>> resultHandler) {
        List<JpsCommandRequest> commandRequestsList = new ArrayList<JpsCommandRequest>();
        JpsGetFiscalPrinterConfigurationCommand command = new JpsGetFiscalPrinterConfigurationCommand();
        command.setPeripheralId(peripheralId);
        command.setSenderType(JpsCommandRequestType.DEVICE);
        commandRequestsList.add(command);
        String json = null;
        try {
            json = JSONUtil.serialize(commandRequestsList.toArray(new JpsCommandRequest[1]));
            context().getLogger(getClass()).info("Sending JpsCommandRequest: " + json);
            vertx.eventBus().request(R.SEND_JPS_COMMAND_REQUEST_URL, json, messageAsyncResult -> {
                if (messageAsyncResult.succeeded()) {
                    context().getLogger(getClass()).info("JpsGetFiscalPrinterConfigurationCommand returned with body: " + messageAsyncResult.result().body().toString());
                    JpsCommandResponse jpsCommandResponse = JSONUtil.deserialize(messageAsyncResult.result().body().toString(), JpsCommandResponse.class);
                    if (jpsCommandResponse.getBody() != null) {
                        JblFiscalPrinterConfiguration configuration = JSONUtil.deserialize(jpsCommandResponse.getBody(), JblFiscalPrinterConfiguration.class);
                        resultHandler.handle(new AsyncResultBuilder().withSuccess().withResult(configuration).build());
                    } else {
                        resultHandler.handle(new AsyncResultBuilder().withFail().build());
                    }
                } else {
                    context().getLogger(getClass()).error("Error sending JpsGetFiscalPrinterConfigurationCommand", messageAsyncResult.cause());
                    resultHandler.handle(new AsyncResultBuilder().withFail().withCause(messageAsyncResult.cause()).build());
                }

            });
        } catch (Exception e) {
            context().getLogger(getClass()).error("Error on serializing JpsGetFiscalPrinterConfigurationCommand", e);
            resultHandler.handle(new AsyncResultBuilder().withFail().withCause(e.getCause()).build());
        }
    }

    private void handleOOA(Message<String> message) {
        context().getLogger(this.getClass()).info("EBBS-1170 - OOA received");
        JblEventExtendedJbl jblEvent = JSONUtil.deserialize(message.body(), JblEventExtendedJbl.class);
        JpsAlrmOutOfOrder jpsEvent = (JpsAlrmOutOfOrder) jblEvent.getJpsEvent();
        context().getLogger(this.getClass()).info("EBBS-1170 - JPS EVENT:" + jpsEvent.toString());
        if ("0x00040000".equalsIgnoreCase(jblEvent.getJpsEvent().getEntityCode())) {
            context().getLogger(this.getClass()).info("EBBS-1170 - FP outoforder received for " + jblEvent.getPeripheralId() + " event: " + jpsEvent.getAlarmPhase().name());
            switch (jpsEvent.getAlarmPhase().name()) {
                case "START":
                    jpsEventService.changePrinterStatus(context(), jblEvent.getPeripheralId(), FiscalPrinterStatusType.Unavailable);
                    break;
                case "END":
                    getFiscalPrinterConfiguration(jblEvent.getPeripheralId(), event -> {
                        if (event.succeeded()) {
                            jpsEventService.changeOrCreatePrinterStatus(context(), jblEvent.getPeripheralId(), event.result());
                        } else {
                            jpsEventService.changePrinterStatus(context(), jblEvent.getPeripheralId(), FiscalPrinterStatusType.Unavailable);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }

    //-----------------------USEFULL METHOD FOR JUNIT TEST CASE------------------------
    public void setJpsEventService(JpsEventService jpsEventService) {
        this.jpsEventService = jpsEventService;
    }

    //----------------------------- BUSINESS LOGIC ------------------------------------

    @Override
    public void doEventManagement(RoutingContext routingContext) {
        String authToken = HttpServerUtils.token(routingContext);
        Long seqTs = HttpServerUtils.getParamLongFromUrl("sequenceNumberTS", routingContext);
        Integer seqGMT = HttpServerUtils.getParamIntegerFromUrl("sequenceNumberGMT", routingContext);
        Integer seqCounter = Math.toIntExact(HttpServerUtils.getParamLongFromUrl("sequenceNumberCounter", routingContext) % Integer.MAX_VALUE);
        String jpsBodyRequest = routingContext.getBodyAsString();
        boolean safe = Boolean.parseBoolean(routingContext.request().headers().get("safe"));
        String session = context(routingContext).serializeJpsSession();
        String peripheralId = HttpServerUtils.getParamStringFromUrl("peripheralId", routingContext);
        session = injectCorrelationId(routingContext, session);
        this.doEventManagement(jpsBodyRequest, seqTs, seqGMT, seqCounter, authToken, peripheralId, session, safe, integerAsyncResult -> {
            if (integerAsyncResult.succeeded()) {
                if (integerAsyncResult.result() == HttpResponseStatus.UNAUTHORIZED.code()) {
                    HttpServerUtils.response(new JsonObject().put("status", "UNKNOWN PERIPHERAL WITH ID -> " + peripheralId).encode(), routingContext, HttpResponseStatus.UNAUTHORIZED.code());
                } else if (integerAsyncResult.result() == HttpResponseStatus.OK.code()) {
                    HttpServerUtils.response(new JsonObject().put("status", "OK").encode(), routingContext, HttpResponseStatus.OK.code());
                } else if (integerAsyncResult.result() == HttpResponseStatus.NOT_FOUND.code()) {
                    HttpServerUtils.response(new JsonObject().put("status", "NOT FOUND").encode(), routingContext, HttpResponseStatus.NOT_FOUND.code());
                } else if (integerAsyncResult.result() == HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                    HttpServerUtils.response(new JsonObject().put("status", "GENERIC ERROR").encode(), routingContext, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                } else if (integerAsyncResult.result() == HttpResponseStatus.BAD_GATEWAY.code()) {
                    HttpServerUtils.response(new JsonObject().put("status", "REQUEST NOT VALID").encode(), routingContext, HttpResponseStatus.BAD_GATEWAY.code());
                }
            } else {
                HttpServerUtils.responseKO(new JsonObject().put("status", "GENERIC ERROR").encode(), routingContext);
            }
        });
    }

    private String injectCorrelationId(RoutingContext routingContext, String session) {
        var correlationId = routingContext.request().headers().get("correlationId");
        if(StringUtils.isEmpty(correlationId))
            return session;
        var o = new JsonObject(session);
        o.put("correlationId", correlationId);
        return o.encode();
    }

    public void doEventManagement(String jpsBodyRequest, Long seqTs, Integer seqGMT, Integer seqCounter, String authToken, String peripheralId, String session, boolean safe, Handler<AsyncResult<Integer>> asyncResultHandler) {
        final long start = System.currentTimeMillis();

        JBLContext context = JBLContext.getInstance();
        Logger logger = context.getLogger(this.getClass());
        authenticatePeripheral(logger, authToken, authenticationResult -> {
            if (authenticationResult.succeeded()) {
                jpsEventService.jfcShiftId(context, authenticationResult.result()).thenAccept(fcjCredentialResult -> {
                    if (fcjCredentialResult.succeeded()) {
                        if (authenticationResult.result() != null) {

                            if (StringUtils.isEmpty(jpsBodyRequest)) {
                                logger.error("jbl.error.jpsEventService.missingBody -> body is empty");
                                asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.BAD_REQUEST.code()).withFail().build());
                                return;
                            }
                            try {

                                logger.info("Event with id: " + seqTs + seqGMT + seqCounter + " received at" + System.currentTimeMillis() + " ms");
                                logger.info("I'm going to manage jpsEventService with url params ts: " + seqTs + " gmt: " + seqGMT + " counter: " + seqCounter + " peripheralId: " + peripheralId);
                                logger.info("I'm going to insert jpsEventService: " + jpsBodyRequest);

                                JsonObject jsonObject;
                                try {
                                    jsonObject = new JsonObject(jpsBodyRequest);
                                } catch (Exception e) {
                                    logger.error("Invalid Json", e);
                                    asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.BAD_REQUEST.code()).withFail().build());
                                    return;
                                }
                                String eventSpecCode = jsonObject.getString("eventSpecCode");
                                String eventType = jsonObject.getString("eventType");
                                MainEventFactory factoryContainer = MainEventFactory.getInstance();

                                try {
                                    JpsEvent prototype = factoryContainer.buildEvent(eventSpecCode, eventType);
                                    JpsEvent jpsEvent = JSONUtil.deserialize(jpsBodyRequest, prototype.getClass());
                                    final JblEventExtendedJbl jblEvent = prototype instanceof JpsEvtAlarm ? new JblAlarmExtended() : new JblEventExtendedJbl();

                                    var sessionJsonObject = new Supplier<JsonObject>() {
                                        private JsonObject wrappedJsonObject;
                                        @Override
                                        public JsonObject get() {
                                            if (wrappedJsonObject == null)
                                                wrappedJsonObject = new JsonObject(session);

                                            return wrappedJsonObject;
                                        }
                                    };

                                    String extendedSession = session;

                                    if (!CollectionUtils.isEmpty(fcjCredentialResult.result())) {
                                        fcjCredentialResult.result().forEach(sessionJsonObject.get()::put);
                                        extendedSession = sessionJsonObject.get().encode();
                                    }

                                    // The value of jpsEvent.getSessionId() is null for JblLogVoucherRead events, and session parameter
                                    // has the sessionId. So lets try to set the value of JpsEvent from session parameter.
                                    if (jpsEvent.getSessionId() == null)
                                    {
                                        try {
                                            var sessionId = sessionJsonObject.get().getString("sessionid");

                                            if (sessionId != null) {
                                                jpsEvent.setSessionId(Long.valueOf(sessionId));
                                                extendedSession = sessionJsonObject.get().encode();
                                            }
                                        }
                                        catch (Exception e) {
                                            logger.error("Failed to parse session id from '" + extendedSession + "'!", e);
                                        }
                                    }

                                    jblEvent.setPeripheralId(peripheralId);
                                    jblEvent.setSession(extendedSession);
                                    jblEvent.setSequenceNumber(new JpsSequenceNumber(seqTs, seqGMT, seqCounter));

                                    jblEvent.setJmsStatus(getJmsStatus(safe, jpsEvent));
                                    jblEvent.setJpsEvent(jpsEvent);

                                    tryProcessRemoteDiscountsOnPaymentCompleted(context, peripheralId, jblEvent, extendedSession, getExtendedSessionAsyncResult -> {
                                        if (!getExtendedSessionAsyncResult.succeeded()) {
                                            logger.error("Invalid result in tryProcessRemoteDiscountsOnPaymentCompleted().",
                                                    getExtendedSessionAsyncResult.cause());
                                        }

                                        // The jblEvent.setJpsEvent(jpsEvent) call above sets the sessionId from jpsEvent.
                                        // The value of sessionid in JpsEvent is null for voucher read events.
                                        // JpsEvent initializes the value from jpsBodyRequest which
                                        // misses the session Id for voucher read events.
                                        // The value of sessionId in JblEvent is initialized from JpsEvent. Therefore it might be null as well.
                                        // The code above tries to initialize JpsEvent.sessionId from sessionid json value in session parameter.
                                        // When debugging, session parameter had sessionId, so in most cases (probably allways) we will have the values of
                                        // JpsEvent.sessionId and JblEvent.sessionId by the time we get here.
                                        // However, if for some reason, sessionId missies in session parameter (or if session parameter is empty),
                                        // the call below will try to set the value of sessionId in JpsEvent and JblEvent objects from the last validation event in
                                        // database. The call to trySetVoucherSessionIdFromDatabase() will be quick, if sessionId is already initialized.
                                        tryGetVoucherSessionIdFromDatabaseIfMissing(context, peripheralId, jblEvent, jpsEvent, tryGetVoucherSessionIdResult -> {

                                            if (tryGetVoucherSessionIdResult.succeeded() && tryGetVoucherSessionIdResult.result() != null) {
                                                jpsEvent.setSessionId(tryGetVoucherSessionIdResult.result());
                                                jblEvent.setSessionId(jpsEvent.getSessionId());
                                            }

                                            CompletableFuture<AsyncResult<JblEventExtendedJbl>> onAfterInsert;
                                            if (jpsEvent instanceof JpsEvtAlarm) {
                                                JpsEvtAlarm jpsAlarm = (JpsEvtAlarm) jpsEvent;
                                                onAfterInsert = jpsEventService.insertAlarm(context, jpsAlarm, jblEvent.getSequenceNumber(), peripheralId, jblEvent.getSession());
                                            } else {
                                                onAfterInsert = jpsEventService.insertEvent(context, jpsEvent, jblEvent.getSequenceNumber(), peripheralId, jblEvent.getSession());
                                            }

                                            onAfterInsert.thenAccept(jblEventAsyncResult -> {

                                                if (jblEventAsyncResult.succeeded()) {

                                                    publish(jblEvent, event -> {
                                                        if (event.succeeded()) {
                                                            if (checkEventIsBreach(jblEvent.getJpsEvent())) {

                                                                JpsPeripheral jpsPeripheral = JpsPeripheral.newInstance(authenticationResult.result().getType());
                                                                //if the peripheral is gateless sending the gate breach alarm too is useless
                                                                if (jpsPeripheral.isEntry() && !jpsPeripheral.isGateless())
                                                                    sendAlarm(context, SpecCodeEnum.JblAlarmLeGateBreach, authenticationResult.result(), jblEvent.getJpsEvent().getSessionId());

                                                                if (jpsPeripheral.isExit() && !jpsPeripheral.isGateless())
                                                                    sendAlarm(context, SpecCodeEnum.JblAlarmLxGateBreach, authenticationResult.result(), jblEvent.getJpsEvent().getSessionId());
                                                            }

                                                            if (jblEvent.getJpsEvent() instanceof JpsOpOptorLogin && ((JpsOpOptorLogin) jblEvent.getJpsEvent()).iamLoginWithError()) {
                                                                sendAlarm(context, SpecCodeEnum.JblAlarmCashierOperatorLoginFailed, authenticationResult.result(), jblEvent.getJpsEvent().getSessionId());
                                                            }

                                                            jblCounterSourceService.processCountingSources(jblEvent);

                                                            logger.info("jpsEventService with ts: " + seqTs + " gmt: " + seqGMT + " counter: " + seqCounter + " peripheralId: " + peripheralId + " correctly stored with elapsed time:" + (System.currentTimeMillis() - start) + " ms");
                                                            asyncResultHandler.handle(Future.succeededFuture(HttpResponseStatus.OK.code()));

                                                        } else {
                                                            logger.info("jpsEventService with ts: " + seqTs + " gmt: " + seqGMT + " counter: " + seqCounter + " peripheralId: " + peripheralId + "returned 404 because publish events is failed" + (System.currentTimeMillis() - start) + " ms");
                                                            asyncResultHandler.handle(Future.succeededFuture(HttpResponseStatus.NOT_FOUND.code()));
                                                        }
                                                    });

                                                } else {
                                                    logger.error(String.format("Insert event/alarm failed with cause: %s", jblEventAsyncResult.cause().toString()));
                                                    asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).withFail().build());
                                                }
                                            });
                                        });
                                    });
                                } catch (hub.ebb.jblcluster.eventservice.generator.InvalidTypeException et) {
                                    logger.error("jbl.error.jpsEventService.unknownType", et);
                                    asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.BAD_REQUEST.code()).withFail().build());
                                } catch (Exception e) {
                                    logger.error("Json Event parsing error" + e.getMessage(), e);
                                    asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.BAD_REQUEST.code()).withFail().build());
                                }

                            } catch (NumberFormatException e) {
                                logger.error("cannot cast some sequenceNumber fields form string", e);
                                asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.BAD_REQUEST.code()).withFail().build());
                            }
                        } else {
                            logger.warn("Peripheral not authenticated");
                            asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.UNAUTHORIZED.code()).withFail().build());
                        }
                    } else {
                        logger.warn("Peripheral not authenticated");
                        asyncResultHandler.handle(new AsyncResultBuilder<Integer>().withResult(HttpResponseStatus.UNAUTHORIZED.code()).withFail().build());
                    }
                });
            } else {
                logger.warn("Peripheral not authenticated");
                asyncResultHandler.handle(Future.succeededFuture(HttpResponseStatus.UNAUTHORIZED.code()));
            }
        });
    }

    private void tryGetVoucherSessionIdFromDatabaseIfMissing(JBLContext jblContext, String peripheralId, JblEventExtendedJbl jblEvent, JpsEvent jpsEvent, Handler<AsyncResult<Long>> asyncResultHandler) {

        if (jblEvent.getSessionId() != null || !SpecCodeEnum.JblLogVoucherRead.equals(jpsEvent.getEventSpecCode())) {
            asyncResultHandler.handle(Future.succeededFuture(null));
            return;
        }

        jblTransactionManager.executeTransaction((conn, transactionBodyResultHandler) -> {

            jblEventDao.findLastValidationEvent(jblContext, conn, peripheralId).thenAccept(findLastValidationEventAsyncResult -> {

                var logger = jblContext.getLogger(getClass());
                transactionBodyResultHandler.handle(Future.succeededFuture());

                JblEvent lastValidationEvent = findLastValidationEventAsyncResult.result();

                if (findLastValidationEventAsyncResult.succeeded() && lastValidationEvent != null) {
                    if (lastValidationEvent.getSessionId() == null)
                        logger.error("The session id in last validation event is null in voucher event for peripheral id=" + peripheralId + "!");

                    asyncResultHandler.handle(Future.succeededFuture(lastValidationEvent.getSessionId()));
                }
                else {
                    logger.error("Failed to load the last validation event to set the session Id in voucher event for peripheral id=" + peripheralId + "!", findLastValidationEventAsyncResult.cause());
                    asyncResultHandler.handle(Future.succeededFuture(null));
                }
            });
        });
    }

    private void tryProcessRemoteDiscountsOnPaymentCompleted(JBLContext jblContext, String peripheralId, JblEvent jblEvent,
                                                            String extendedSessionText,
                                                            Handler<AsyncResult> asyncResultHandler) {

        if (SpecCodeEnum.JpsOpUsrPayCompl.getValue() != (jblEvent.getEventSpecCode())) {
            asyncResultHandler.handle(Future.succeededFuture());
            return;
        }

        var peripheralRemoteDiscountSessionData = this.remoteDiscountSessionData.getPeripheralRemoteDiscountSessionData(peripheralId);
        peripheralRemoteDiscountSessionData.getOrLoadRemoteDiscounts(jblContext, jblEvent.getSessionId(), getOrLoadRemoteDiscountsResult -> {

            var remoteDiscounts = getOrLoadRemoteDiscountsResult.result();
            if (!getOrLoadRemoteDiscountsResult.succeeded() || remoteDiscounts == null ) {
                jblContext.getLogger(JpsEventVerticle.class).error(String.format("Failed to add remote discounts to session data on payment complete=%s, session Id=%s.", peripheralId, jblEvent.getSession()),
                        getOrLoadRemoteDiscountsResult.cause());

                asyncResultHandler.handle(Future.succeededFuture());
                return;
            }
            else if (remoteDiscounts.size() > 0) {

                final JsonObject extendedSession = new JsonObject(extendedSessionText);

                extendedSession.put(SessionFields.TOTAL_REMOTE_DISCOUNTS, remoteDiscounts.size());

                for (var remoteDiscountInd = 0; remoteDiscountInd < remoteDiscounts.size(); ++remoteDiscountInd) {
                    var remoteDiscount = remoteDiscounts.get(remoteDiscountInd);

                    extendedSession.put(SessionFields.REMOTE_DISCOUNT_ID + "_"+remoteDiscountInd, remoteDiscount.getDiscountId())
                            .put(SessionFields.REMOTE_DISCOUNT_ORIGINAL_AMOUNT + "_"+remoteDiscountInd, remoteDiscount.getOriginalAmount())
                            .put(SessionFields.REMOTE_DISCOUNT_VALUE_DISCOUNTED + "_"+remoteDiscountInd, remoteDiscount.getValueDiscounted())
                            .put(SessionFields.REMOTE_DISCOUNT_PERCENTAGE_DISCOUNTED + "_"+remoteDiscountInd, remoteDiscount.getDiscountPercentage());
                }

                jblEvent.setSession(extendedSession.encode());

                peripheralRemoteDiscountSessionData.onPaymentCompleted(jblContext, jblEvent.getSessionId(), onPaymentCompletedAsyncHandler -> {
                    if (!onPaymentCompletedAsyncHandler.succeeded()) {
                        jblContext.getLogger(JpsEventVerticle.class).error(String.format("Failed to set the status of remote discounts on payment completed. Peripheral id='%s', Session Id=%s.",
                                peripheralId, jblEvent.getSessionId()), onPaymentCompletedAsyncHandler.cause());
                    }

                    asyncResultHandler.handle(Future.succeededFuture());
                });
            }
            else {
                asyncResultHandler.handle(Future.succeededFuture());
            }
        });
    }

    @Override
    public void sendEvent(JpsEventDto event, Handler<AsyncResult<Void>> asyncResultHandler) {
        if (event == null) {
            asyncResultHandler.handle(Future.failedFuture(new IllegalArgumentException("Event must be not null")));
            return;
        }

        this.doEventManagement(event.getJpsBodyRequest(), event.getSeqTs(), event.getSeqGMT(), event.getSeqCounter(), event.getAuthToken(), event.getPeripheralId(), event.getSession(), event.isSafe(), t -> {
            if (t.succeeded()) {
                if (t.result() == HttpResponseStatus.OK.code()) {
                    asyncResultHandler.handle(Future.succeededFuture());
                } else {
                    asyncResultHandler.handle(Future.failedFuture("Error code -> " + t.result()));
                }
            } else {
                asyncResultHandler.handle(Future.failedFuture(t.cause()));
            }
        });
    }


    private JmsStatus getJmsStatus(boolean safe, JpsEvent jpsEvent) {
        JmsStatus status = safe ? JmsStatus.NOT_SENT : JmsStatus.SENT;
        if (status.equals(JmsStatus.SENT)) {
            status = JmsStatus.valueOf(jpsEvent.getJmsStatus());
        }
        return status;
    }

    private JmsStatus getJmsStatus(RoutingContext routingContext, JpsEvent jpsEvent) {
        JmsStatus status = Boolean.parseBoolean(routingContext.request().headers().get("safe")) ? JmsStatus.NOT_SENT : JmsStatus.SENT;
        if (status.equals(JmsStatus.SENT)) {
            status = JmsStatus.valueOf(jpsEvent.getJmsStatus());
        }
        return status;
    }

    protected void publish(JblEventExtendedJbl jblEvent, Handler<AsyncResult<Void>> resultHandler) {
        String eventBus = jblEvent.eventBusPublishUrl();
        if (!StringUtils.isEmpty(eventBus)) {
            String json = "";
            try {
                json = JSONUtil.serialize(jblEvent);
            }
            catch (Exception e) {
                context().getLogger(getClass()).error("Error on serializing jblEvent", e);
                resultHandler.handle(Future.failedFuture(e));
                return;
            }
            vertx.eventBus().publish(eventBus, json);
            resultHandler.handle(Future.succeededFuture());
        } else {
            resultHandler.handle(Future.succeededFuture());
        }

    }

    public boolean checkEventIsBreach(JpsEvent event) {
        if (!(event instanceof JpsOpUsrTransit))
            return false;

        JpsOpUsrTransit<?> transit = (JpsOpUsrTransit<?>) event;
        return transit.isBreach();

    }

    public void prepareGatelessAlarm(JBLContext context, SpecCodeEnum eventSpecCode, JpsAuthenticatedPeripheral auth, Long sessionId, Long jmsOperationId) {
        Map<String, String> auxMap = new HashMap<>();
        auxMap.put("jmsOperationId", jmsOperationId.toString());
        this.sendAlarm(context, eventSpecCode, auth, sessionId, auxMap);
    }

    protected void sendAlarm(JBLContext context, SpecCodeEnum eventSpecCode, JpsAuthenticatedPeripheral auth, Long sessionId) {
        sendAlarm(context, eventSpecCode, auth, sessionId, new HashMap<>());
    }

    protected void sendAlarm(JBLContext context, SpecCodeEnum eventSpecCode, JpsAuthenticatedPeripheral auth, Long sessionId, Map<String, String> params) {

        JblAlarmExtended jblAlarm = new JblAlarmExtended();
        jblAlarm.setSequenceNumber(new JpsSequenceNumber(DateUtils.getUnixTSInMillis(), DateUtils.getGMTInMinutes(), EventSequenceNumberGenerator.getInstance().nextInt()));
        jblAlarm.setJmsStatus(JmsStatus.NOT_SENT);
        jblAlarm.setPeripheralId(auth.getPeripheralId());
        String session = context.serializeJpsSession();

        JpsEvtAlarm jpsEvtAlarm = new JpsEvtAlarm();

        jpsEvtAlarm.setEventSpecCode(eventSpecCode);
        jpsEvtAlarm.setEventType(JpsEvtAlarm.DICTIONARY_TYPE_CODE);
        jpsEvtAlarm.setSessionId(sessionId);
        jpsEvtAlarm.setEntityCode(auth.getPeripheralId());

        if (JpsPeripheral.newInstance(auth.getType()).isGateless()) {
            if (config().getBoolean("LprAlarmsWithReason.enable", false)) {
                jpsEvtAlarm.setAlarmPhase(JpsAlarmPhase.ONESHOT_TAKE_IN_CARE_ALARM_CLEARING_WITH_REASON_CONFIRMATION_NEEDED);
            } else {
                jpsEvtAlarm.setAlarmPhase(JpsAlarmPhase.ONESHOT_TAKE_IN_CARE_ALARM_CLEARING_CONFIRMATION_NEEDED);
            }
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Higher);
        } else {
            jpsEvtAlarm.setAlarmPhase(JpsAlarmPhase.ONESHOT_TAKE_IN_CARE);
            jpsEvtAlarm.setSeverity(JpsEventSeverity.Medium);
        }

        jpsEvtAlarm.setPeripheralType(auth.getType());

        jblAlarm.setJpsEvent(jpsEvtAlarm);

        Long jmsOperationId = null;
        if (params.get("jmsOperationId") != null) {
            jmsOperationId = Long.parseLong(params.get("jmsOperationId"));
            jpsEvtAlarm.setJmsOperationId(jmsOperationId);
        }

        jpsEventService.insertAlarm(context, jpsEvtAlarm, jblAlarm.getSequenceNumber(), auth.getPeripheralId(), session, jmsOperationId).thenAccept(jpsEventAsyncResult -> {

            if (jpsEventAsyncResult.succeeded()) {
                context().getLogger(this.getClass()).info(jpsEvtAlarm + " correctly sent");
            } else {
                context().getLogger(this.getClass()).error(jpsEvtAlarm + " error on sending", jpsEventAsyncResult.cause());
            }
        });

    }

    private void authenticatePeripheral(Logger logger, String authToken, Handler<AsyncResult<JpsAuthenticatedPeripheral>> resultHandler) {
        logger.info("Authenticating peripheral token: " + authToken);
        JpsAuthenticatedPeripheral authenticatedPeripheral = authenticatedPeripheralCache.get(authToken);
        if (authenticatedPeripheral == null) {
            authenticatedPeripheralAPI.doCheckAuthenticationJPS(authToken, l -> {
                if (l.succeeded() && l.result() != null) {
                    JpsAuthenticatedPeripheral peripheral = JpsAuthenticatedPeripheral.fromJson(l.result());
                    authenticatedPeripheralCache.put(peripheral.getToken(), peripheral);
                    logger.info("Authenticated token -> " + authToken + " peripheral id -> " + peripheral.getPeripheralId());
                    resultHandler.handle(new AsyncResultBuilder<JpsAuthenticatedPeripheral>().withResult(peripheral).withSuccess().build());
                } else {
                    logger.error(" Error on authentication token -> " + authToken, l.cause());
                    resultHandler.handle(new AsyncResultBuilder<JpsAuthenticatedPeripheral>().withFail().withCause(l.cause()).build());
                }
            });
        } else {
            logger.info("Hit Authenticated cache form token: " + authToken + " " + authenticatedPeripheral.getPeripheralId());
            resultHandler.handle(new AsyncResultBuilder<JpsAuthenticatedPeripheral>().withResult(authenticatedPeripheral).withSuccess().build());
        }
    }

    //send alarm on peripheral time mismstch
    private void sendAlarmForClockAlignment(Message<String> tMessage) {
        Map<String, String> param = JSONUtil.deserialize(tMessage.body(), Map.class);
        JBLContext instance = JBLContext.getInstance();
        try {
            if (param.get("status").equals("KO"))
                JpsEventService.sendAlarm(instance, serviceDiscoveryClient, JblEventBundle.JblTimePeripheralMismatch, param.get("peripheralId"), param.get("type"), JpsAlarmPhase.START, 1L, param);
            else
                JpsEventService.clearAlarm(instance, serviceDiscoveryClient, JblEventBundle.JblTimePeripheralMismatch, param.get("peripheralId"), param.get("type"), 1L);
        } catch (InvalidJpsEventTypeException e) {
            instance.getLogger(getClass()).error("Error sending ->sendAlarmForClockAlignment", e);
        }
    }

    private void sendAlarmForGatelessDeviceStatus(Message<String> tMessage) {
        Map<String, String> param = JSONUtil.deserialize(tMessage.body(), Map.class);
        JBLContext instance = JBLContext.getInstance();
        try {
            if (param.get("status").equals("KO"))
                JpsEventService.sendAlarm(instance, serviceDiscoveryClient, SpecCodeEnum.JblAlarmGatelessOffline.getValue(), param.get("peripheralId"), null, JpsAlarmPhase.ONESHOT_TAKE_IN_CARE, 1L, param);
            else
                JpsEventService.clearAlarm(instance, serviceDiscoveryClient, SpecCodeEnum.JblAlarmGatelessOffline.getValue(), param.get("peripheralId"), null, 1L);
        } catch (InvalidJpsEventTypeException e) {
            instance.getLogger(getClass()).error("Error sending ->sendAlarmForGatelessDeviceStatus", e);
        }
    }

    //send alarm on peripheral offline
    private void sendAlarmForPeripheralOffline(Message<String> tMessage) {
        Map<String, String> param = JSONUtil.deserialize(tMessage.body(), Map.class);
        JBLContext instance = JBLContext.getInstance();
        try {
            if (param.get("status").equals("KO"))
                JpsEventService.sendAlarm(instance, serviceDiscoveryClient, JblEventBundle.JblAlarmPeripheralOffline, param.get("peripheralId"), param.get("type"), JpsAlarmPhase.START, 1L, param);
            else
                JpsEventService.clearAlarm(instance, serviceDiscoveryClient, JblEventBundle.JblAlarmPeripheralOffline, param.get("peripheralId"), param.get("type"), 1L);
        } catch (InvalidJpsEventTypeException e) {
            instance.getLogger(getClass()).error("Error sending ->sendAlarmForPeripheralOffline", e);
        }
    }
}
