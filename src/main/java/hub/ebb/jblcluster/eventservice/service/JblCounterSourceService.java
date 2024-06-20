package hub.ebb.jblcluster.eventservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import hub.jbl.common.lib.JblAPIEventBusBundle;
import hub.jbl.common.lib.PeripheralsHex;
import hub.jbl.common.lib.SessionFields;
import hub.jbl.common.lib.api.counters.CountersAPI;
import hub.jbl.common.lib.builder.AsyncResultBuilder;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.utils.handler.PromiseUtil;
import hub.jbl.common.lib.utils.json.JsonUtilsExtended;
import hub.jbl.common.services.AbstractJblService;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.core.dto.jbl.ActionSetActivatorDTO;
import hub.jbl.core.dto.jbl.ActionSetActivatorEventType;
import hub.jbl.core.dto.jbl.ActionSetActivatorType;
import hub.jbl.core.dto.jbl.CounterSourceLinkDTO;
import hub.jbl.core.dto.jps.event.*;
import hub.jbl.dao.JblParkCounterSourceDao;
import hub.jbl.dao.JblParkCounterSourceFilterDao;
import hub.jbl.dao.JblParkCounterSourceFilterSetDao;
import hub.jbl.dao.JblParkNodeDao;
import hub.jbl.dao.util.SQLConnectionWrapper;
import hub.jbl.entity.parknode.*;
import hub.ebb.jblcluster.eventservice.model.JblEventExtendedJbl;
import hub.ebb.jblcluster.eventservice.service.jmsMapper.JblParkCounterV2Mapper;
import hub.jms.common.model.parking.DeviceType;
import hub.jms.common.model.parking.NodeType;
import hub.jms.common.model.utils.JSONUtil;
import hub.jms.common.model.utils.Tuple;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import hub.jbl.model.authentication.common.JpsPeripheral;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static hub.jbl.core.dto.jps.event.JpsCountingLinkInputType.*;

public class JblCounterSourceService extends AbstractJblService<JblParkCounterV2, JblParkCounterV2> implements CountersAPI {

    private List<JblParkNode> parkNodes = new ArrayList<>();
    private Map<String, List<JblParkCounterSource>> counterSources = new HashMap<>();
    private List<JblParkCounterSourceFilterSet> filterSets = new ArrayList<>();
    private Boolean isInitialized = false;
    private JBLContext context;

    final private Vertx vertx;

    @Autowired
    private JblTransactionManager jblTransactionManager;
    @Autowired
    private JblParkNodeDao jblParkNodeDao;
    @Autowired
    private JblParkCounterSourceDao jblParkCounterSourceDao;
    @Autowired
    private JblParkCounterSourceFilterSetDao jblParkCounterSourceFilterSetDao;
    @Autowired
    private JblParkCounterSourceFilterDao jblParkCounterSourceFilterDao;

    public JblCounterSourceService(Vertx vertx) {
        super(new JblParkCounterV2Mapper());
        this.vertx = vertx;
        this.context = JBLContext.getInstance();
    }

    public void processCountingSources(JblEventExtendedJbl jblEvent) {
        processCountingSources(jblEvent, null, asyncResult -> {
        });
    }

    private void processCountingSources(JblEventExtendedJbl jblEvent, JblInternalCountingEvent countingEvent, Handler<AsyncResult<Void>> resultHandler) {
        getCountingSourceOutputsByJblEvent(jblEvent, countingEvent, counterSources -> {
            if (counterSources.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
                counterSources.result().forEach(cs -> {
                    vertx.eventBus().publish(JblAPIEventBusBundle.PUB_COUNTING_OUTPUT_EVT, JSONUtil.serialize(cs.first));
                    if (cs.second != null)
                        vertx.eventBus().publish(JblAPIEventBusBundle.PUB_ACTION_SET_ACTIVATOR_EVT, JSONUtil.serialize(cs.second));
                });
            } else {
                resultHandler.handle(new AsyncResultBuilder<Void>().withFail().withCause(counterSources.cause()).build());
            }
        });
    }

    private void getCountingSourceOutputsByJblEvent(JblEventExtendedJbl jblEvent, JblInternalCountingEvent countingEvent, Handler<AsyncResult<List<Tuple<JpsCountingOutputEvent, ActionSetActivatorDTO>>>> resultHandler) {
        String eventKey = null;

        if (countingEvent != null)
            eventKey = countingEvent.getClass().getSimpleName().toLowerCase();
        else if (jblEvent.getJpsEvent() != null)
            eventKey = jblEvent.getJpsEvent().getClass().getSimpleName().toLowerCase();

        if (counterSources.containsKey(eventKey)) {
            List<Long> parkingNodeIds = new ArrayList<>();

            if (countingEvent != null && countingEvent.getParkNodeId() != null)
                parkingNodeIds.add(countingEvent.getParkNodeId());

            if (jblEvent.getPeripheralId() != null) {
                JblParkNode peripheralNode = parkNodes.stream().filter(n -> n.getIdentifier() != null && n.getIdentifier().equals(jblEvent.getPeripheralId())).findFirst().orElse(null);

                if (peripheralNode != null) {
                    parkingNodeIds.addAll(Arrays.stream(new Long[]{
                            ObjectUtils.firstNonNull(peripheralNode.getToId(), peripheralNode.getFromId()),
                            peripheralNode.getFromId() == null || peripheralNode.getToId() == null ? peripheralNode.getParentId() : null})
                            .filter(Objects::nonNull).collect(Collectors.toList()));
                }

                getCountingSourceOutputsByJblEvent(jblEvent, countingEvent, eventKey, peripheralNode, parkingNodeIds, resultHandler);
            } else if (countingEvent != null) {
                getCountingSourceOutputsByJblEvent(jblEvent, countingEvent, eventKey, null, parkingNodeIds, resultHandler);
            } else {
                resultHandler.handle(Future.succeededFuture(new ArrayList<>()));
            }
        } else {
            resultHandler.handle(Future.succeededFuture(new ArrayList<>()));
        }
    }

    private void getCountingSourceOutputsByJblEvent(JblEventExtendedJbl jblEvent, JblInternalCountingEvent countingEvent, String eventKey, JblParkNode peripheralNode, List<Long> parkingNodeIds, Handler<AsyncResult<List<Tuple<JpsCountingOutputEvent, ActionSetActivatorDTO>>>> resultHandler) {
        final List<Tuple<JpsCountingOutputEvent, ActionSetActivatorDTO>> result = new ArrayList<>();
        final JsonNode jpsEvent = JsonUtilsExtended.readJsonTree(jblEvent.getJson());
        final JsonNode cntEventParams = countingEvent != null ? JsonUtilsExtended.readJsonTree(JsonObject.mapFrom(countingEvent.getParams()).encode()) : null;
        final JpsPeripheral peripheral;
        if (jblEvent.getJpsEvent() != null && jblEvent.getJpsEvent().getPeripheralType() != null)
            peripheral = JpsPeripheral.newInstance(jblEvent.getJpsEvent().getPeripheralType());
        else
            peripheral = null;

        for (JblParkCounterSource source : counterSources.get(eventKey)) {
            context.getLogger(this.getClass()).debug(String.format("Checking counter source - eventKey: %s, source: '%s'", eventKey, source.getName()));
            if (Boolean.TRUE.equals(source.isEnabled() &&
                    (peripheralNode != null && source.getPeripheralNodeId() != null && source.getPeripheralNodeId().equals(peripheralNode.getId())) ||
                    (parkingNodeIds.stream().anyMatch(nodeId -> nodeId.equals(source.getNodeId())) && isPeripheralTypeEqualSourceDeviceType(peripheral, source))) ||
                    (source.getNodeId() == null && source.getPeripheralNodeId() == null && isPeripheralTypeEqualSourceDeviceType(peripheral, source))) {
                if (source.getSourceFilterSet().getSourceFilters().stream().allMatch(sourceFilter -> {
                    String compareValue;
                    String propertyPath = sourceFilter.getPropertyPath().trim();

                    if (propertyPath.endsWith(")"))
                        compareValue = executeSourceFilterFunction(countingEvent, jblEvent, propertyPath);
                    else {
                        compareValue = readJsonNodeValue(jpsEvent, propertyPath);
                        if (compareValue == null && cntEventParams != null)
                            compareValue = readJsonNodeValue(cntEventParams, propertyPath);
                    }

                    boolean matched = false;

                    if (compareValue != null) {
                        if (sourceFilter.getStartValue() != null || sourceFilter.getEndValue() != null) {
                            matched = true;
                            if (sourceFilter.getStartValue() != null)
                                matched = (sourceFilter.getStartValue().compareToIgnoreCase(compareValue) <= 0);
                            if (sourceFilter.getEndValue() != null)
                                matched &= (sourceFilter.getEndValue().compareToIgnoreCase(compareValue) >= 0);
                        } else
                            matched = compareValue.equalsIgnoreCase(sourceFilter.getPropertyValue());
                    }

                    if (Boolean.TRUE.equals(sourceFilter.isLogicalNot()))
                        matched = !matched;

                    context.getLogger(this.getClass()).debug(String.format("\t\t- filter propertyPath: %s, filter propertyValue: %s, event value: %s, matched: %s", propertyPath, sourceFilter.getPropertyValue(), compareValue, matched));

                    return matched;
                })) {
                    JpsCountingOutputEvent countingOutputEvent = new JpsCountingOutputEvent();
                    Long actionSetId = Boolean.TRUE.equals(source.isTriggerActionSetActive()) ? source.getTriggerActionSetId() : null;
                    countingOutputEvent.setOutputType(JpsCountingLinkOutputType.SourceEvent);
                    countingOutputEvent.setOutputId(source.getId());

                    if (countingEvent != null)
                        countingOutputEvent.setParams(JsonObject.mapFrom(countingEvent.getParams()));

                    ActionSetActivatorDTO activator = null;

                    if (actionSetId != null) {
                        activator = new ActionSetActivatorDTO();
                        activator.setActionSetActivatorType(ActionSetActivatorType.CounterSource);
                        activator.setActionSetActivatorId(source.getId());
                        activator.setActionSetId(actionSetId);
                        activator.setActionSetActivatorEventType(ActionSetActivatorEventType.CounterSourceTriggered);
                    }

                    result.add(new Tuple<>(countingOutputEvent, activator));
                }
            }
        }

        resultHandler.handle(Future.succeededFuture(result));
    }

    public void onParkingNodeUpdate(Message<Object> message) {
        parkNodes = Arrays.asList(JSONUtil.deserialize(message.body().toString(), JblParkNode[].class));
        message.reply("OK");
    }

    public void initializeService(Handler<AsyncResult<Void>> resultHandler) {
        if (!isInitialized) {
            isInitialized = true; // always set true even in case of failure in order to avoid endless loops
            // in the attempt to load counting sources

            JBLContext context = JBLContext.getInstance();

            loadParkingTree(context, parkTreeResult -> {
                if (parkTreeResult.succeeded()) {
                    parkNodes.addAll(parkTreeResult.result());

                    loadSourceConfiguration(context, sourceConfigResult -> {
                        if (sourceConfigResult.succeeded()) {
                            resultHandler.handle(new AsyncResultBuilder<Void>().withSuccess().build());
                        } else {
                            resultHandler.handle(new AsyncResultBuilder<Void>().withFail().withCause(sourceConfigResult.cause()).build());
                        }
                    });
                } else {
                    resultHandler.handle(new AsyncResultBuilder<Void>().withFail().withCause(parkTreeResult.cause()).build());
                }
            });
        } else {
            resultHandler.handle(new AsyncResultBuilder<Void>().withSuccess().build());
        }
    }

    private void loadParkingTree(JBLContext context, Handler<AsyncResult<List<JblParkNode>>> resultHandler) {
        jblTransactionManager.executeTransaction((conn, transactionBodyResultHandler) -> jblParkNodeDao.findAll(context, conn, nodes -> {
            transactionBodyResultHandler.handle(Future.succeededFuture());

            if (nodes.succeeded()) {
                resultHandler.handle(new AsyncResultBuilder<List<JblParkNode>>().withSuccess().withResult(nodes.result()).build());
            } else {
                resultHandler.handle(new AsyncResultBuilder<List<JblParkNode>>().withFail().withCause(nodes.cause()).build());
                context.getLogger(getClass()).error("JblCounterService - Unable to retrieve records from park_node");
            }
        }));
    }

    private void loadSourceConfiguration(JBLContext context, Handler<AsyncResult<Void>> resultHandler) {
        jblTransactionManager.executeTransaction((conn, transactionBodyResultHandler) -> {
            jblParkCounterSourceDao.findAll(context, conn, sourcesResult -> {
                if (sourcesResult.succeeded()) {
                    jblParkCounterSourceFilterSetDao.findAll(context, conn, filterSetResult -> {
                        if (filterSetResult.succeeded()) {
                            filterSets = filterSetResult.result();
                            jblParkCounterSourceFilterDao.findAll(context, conn, filterResult -> {
                                if (filterResult.succeeded()) {
                                    buildCounterSourceHierarchy(sourcesResult.result(), filterResult.result());
                                    transactionBodyResultHandler.handle(Future.succeededFuture());
                                    resultHandler.handle(new AsyncResultBuilder<Void>().withSuccess().build());
                                } else {
                                    transactionBodyResultHandler.handle(new AsyncResultBuilder<>().withFail().withCause(filterResult.cause()).build());
                                    resultHandler.handle(new AsyncResultBuilder<Void>().withFail().withCause(filterResult.cause()).build());
                                }
                            });
                        } else {
                            transactionBodyResultHandler.handle(new AsyncResultBuilder<>().withFail().withCause(filterSetResult.cause()).build());
                            resultHandler.handle(new AsyncResultBuilder<Void>().withFail().withCause(filterSetResult.cause()).build());
                        }
                    });
                } else {
                    transactionBodyResultHandler.handle(new AsyncResultBuilder<>().withFail().withCause(sourcesResult.cause()).build());
                    resultHandler.handle(new AsyncResultBuilder<Void>().withFail().withCause(sourcesResult.cause()).build());
                }
            });
        });
    }

    private void buildCounterSourceHierarchy(List<JblParkCounterSource> sources, List<JblParkCounterSourceFilter> filter) {
        filterSets.forEach(fs -> sources.stream().filter(s -> s.getSourceFilterSetId() == fs.getId())
                .collect(Collectors.toList()).forEach(s -> s.setSourceFilterSet(fs)));

        Map<Long, List<JblParkCounterSourceFilter>> filterByFilterSet =
                filter.stream().collect(Collectors.groupingBy(JblParkCounterSourceFilter::getSourceFilterSetId));

        for (Map.Entry<Long, List<JblParkCounterSourceFilter>> filterSetItem : filterByFilterSet.entrySet()) {
            filterSets.stream().filter(fs -> fs.getId() == filterSetItem.getKey()).findFirst().ifPresent(filterSet -> filterSet.setSourceFilters(filterSetItem.getValue()));
        }

        counterSources = sources.stream().collect(Collectors.groupingBy(x -> x.getSourceFilterSet().getEventName().trim().toLowerCase()));
    }

    private String executeSourceFilterFunction(JblInternalCountingEvent countingEvent, JblEventExtendedJbl jblEvent, String functionCall) {
        try {
            int posOpenBracket = functionCall.indexOf('(');
            int posCloseBracket = functionCall.lastIndexOf(')');

            if (posOpenBracket >= 0 && posCloseBracket > posOpenBracket) {
                String functionName = functionCall.substring(0, posOpenBracket);

                switch (functionName.toLowerCase()) {
                    case "isdirectorforcedtransit":
                        if (jblEvent != null)
                            return String.valueOf(isDirectOrForcedTransit(jblEvent));
                    case "istransientorexternaltransit":
                        if (jblEvent != null)
                            return String.valueOf(isTransientOrExternalTransit(jblEvent));
                    case "istransienttransit":
                        if (jblEvent != null)
                            return String.valueOf(isTransientTransit(jblEvent));
                    case "issubscriptiontransit":
                        if (jblEvent != null)
                            return String.valueOf(isSubscriptionTransit(jblEvent));
                    case "isotherstransit":
                        if (jblEvent != null)
                            return String.valueOf(isOthersTransit(jblEvent));
                    case "isextvalidatortransit":
                        if (jblEvent != null)
                            return String.valueOf(isExtValidatorTransit(jblEvent));
                    case "isproductprofile":
                        if (jblEvent != null)
                            return String.valueOf(getProductProfile(jblEvent));
                    case "isuserpasstype":
                    case "isusrpasstype":
                        if (jblEvent != null)
                            return String.valueOf(getUsrPassType(jblEvent));
                    case "isreservedtransit":
                        if (jblEvent != null)
                            return String.valueOf(isReservedTransit(jblEvent));
                }

                if (countingEvent != null) {
                    JblInternalCountingEventType countingEventType = getInternalCountingEventTypeByFilterProperty(functionName);
                    return String.valueOf(countingEventType.equals(countingEvent.getCountingEventType()));
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    private JblInternalCountingEventType getInternalCountingEventTypeByFilterProperty(String filterProperty) {
        String lowerFilterProperty = filterProperty.toLowerCase();

        if (lowerFilterProperty.startsWith("istransientconvertedtoanonymoussubscription"))
            return JblInternalCountingEventType.TransientConvertedToAnonymousSubscription;
        if (lowerFilterProperty.startsWith("isplateresettransiententrance"))
            return JblInternalCountingEventType.PlateResetTransientEntrance;
        if (lowerFilterProperty.startsWith("isfloorcounterevent"))
            return JblInternalCountingEventType.FloorCounterEvent;
        if (lowerFilterProperty.startsWith("isbookingreserveplaceevent"))
            return JblInternalCountingEventType.BookingReservePlaceEvent;

        return JblInternalCountingEventType.NotSpecified;
    }

    private String readJsonNodeValue(JsonNode jpsEvent, String propertyPath) {
        if (jpsEvent != null) {
            JsonNode jsonNode = jpsEvent.at("/" + propertyPath.replace('.', '/'));
            if (jsonNode.isValueNode())
                return jsonNode.asText();
        }

        return null;
    }

    private boolean isTransientOrExternalTransit(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getJpsEvent() instanceof JpsOpUsrTransit) {
            JpsOpUsrTransit jpsTransit = (JpsOpUsrTransit) jblEvent.getJpsEvent();
            return jpsTransit.getUsrPass() != null && (JpsUsrPassType.Transient.equals(jpsTransit.getUsrPass().getUsrType()) || JpsUsrPassType.Extern.equals(jpsTransit.getUsrPass().getUsrType()));
        }

        return false;
    }

    private boolean isTransientTransit(JblEventExtendedJbl jblEvent) {
        return JpsUsrPassType.Transient.equals(getUsrPassType(jblEvent));
    }

    private boolean isSubscriptionTransit(JblEventExtendedJbl jblEvent) {
        return JpsUsrPassType.Subscription.equals(getUsrPassType(jblEvent));
    }

    private boolean isOthersTransit(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getJpsEvent() instanceof JpsLogUsrPass) {
            if (jblEvent.getSession() != null && !StringUtils.isEmpty(jblEvent.getSession())) {
                JsonObject sessionObject = new JsonObject(jblEvent.getSession());
                if (sessionObject.containsKey(SessionFields.PRODUCT_PROFILE_TYPE) && !sessionObject.getString(SessionFields.PRODUCT_PROFILE_TYPE).equals(ProductProfileType.UNKNOWN.name()))
                    return false;
            }

            JpsOpUsrTransit jpsTransit = (JpsOpUsrTransit) jblEvent.getJpsEvent();
            return jpsTransit.getUsrPass() == null || JpsUsrPassType.Unknown.equals(jpsTransit.getUsrPass().getUsrType());
        }

        return false;
    }

    private boolean isExtValidatorTransit(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getJpsEvent() instanceof JpsLogUsrPass) {
            JpsLogUsrPass jpsLogUsrPass = (JpsLogUsrPass) jblEvent.getJpsEvent();
            if (jpsLogUsrPass.getMediaType().equals(JpsUsrPassMediaType.ExtValidator))
                return true;
        }

        if (jblEvent.getJpsEvent() instanceof JpsOpUsrTransit) {
            JpsOpUsrTransit jpsTransit = (JpsOpUsrTransit) jblEvent.getJpsEvent();
            return jpsTransit.getUsrPass() != null && jpsTransit.getUsrPass().getMediaType().equals(JpsUsrPassMediaType.ExtValidator);
        }

        return false;
    }

    private boolean isDirectOrForcedTransit(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getJpsEvent() instanceof JpsOpUsrTransit) {
            JpsOpUsrTransit jpsTransit = (JpsOpUsrTransit) jblEvent.getJpsEvent();

            return jpsTransit.getTransitType().equals(JpsTransitType.SuccessfulDirect) ||
                    jpsTransit.getTransitType().equals(JpsTransitType.BreachDirect) ||
                    (jpsTransit.getTransitType().equals(JpsTransitType.Force) && isCountableForceTransitType(jpsTransit, jblEvent.getSession()));
        }

        return false;
    }

    // EBBS-7779 force entry for external ticket with replacement of transient,
    // for which replacement it is issued void at entry for transient,
    // counter should not change
    private boolean isCountableForceTransitType(JpsOpUsrTransit jpsTransit, String session) {
        if (jpsTransit.getUsrPass() != null && !StringUtils.isBlank(jpsTransit.getUsrPass().getOldLstUid())) {
            if (!StringUtils.isBlank(session)) {
                JsonObject sessionObject = new JsonObject(session);
                if (sessionObject.containsKey(SessionFields.EXTERNAL_TICKET))
                    return StringUtils.isBlank(sessionObject.getString(SessionFields.EXTERNAL_TICKET));
            }
        }

        return true;
    }

    private boolean isReservedTransit(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getJpsEvent() instanceof JpsOpUsrTransit) {
            JpsOpUsrTransit jpsTransit = (JpsOpUsrTransit) jblEvent.getJpsEvent();
            return jpsTransit.getUsrPass() != null && jpsTransit.getUsrPass().isReserved();
        }

        return false;
    }

    private ProductProfileType getProductProfile(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getSession() != null && !StringUtils.isEmpty(jblEvent.getSession())) {
            JsonObject sessionObject = new JsonObject(jblEvent.getSession());
            if (sessionObject.containsKey(SessionFields.PRODUCT_PROFILE_TYPE))
                return ProductProfileType.valueOf(sessionObject.getString(SessionFields.PRODUCT_PROFILE_TYPE));
        }

        if (jblEvent.getJpsEvent() instanceof JpsLogUsrPass) {
            JpsLogUsrPass usrPass = (JpsLogUsrPass) jblEvent.getJpsEvent();
            if (usrPass.getPrdType() != null)
                return usrPass.getPrdType();
        }

        if (jblEvent.getJpsEvent() instanceof JpsOpUsrTransit) {
            JpsLogUsrPass usrPass = ((JpsOpUsrTransit)(jblEvent.getJpsEvent())).getUsrPass();
            if (usrPass.getPrdType() != null)
                return usrPass.getPrdType();
        }

        return ProductProfileType.UNKNOWN;
    }

    private JpsUsrPassType getUsrPassType(JblEventExtendedJbl jblEvent) {
        if (jblEvent.getSession() != null && !StringUtils.isEmpty(jblEvent.getSession())) {
            JsonObject sessionObject = new JsonObject(jblEvent.getSession());
            if (sessionObject.containsKey(SessionFields.JPS_USRPASS_TYPE))
                return JpsUsrPassType.valueOf(sessionObject.getString(SessionFields.JPS_USRPASS_TYPE));
        }

        if (jblEvent.getJpsEvent() instanceof JpsLogUsrPass) {
            JpsLogUsrPass usrPass = (JpsLogUsrPass) jblEvent.getJpsEvent();
            if (usrPass.getUsrType() != null)
                return usrPass.getUsrType();
        }

        return JpsUsrPassType.Unknown;
    }

    private List<DeviceType> getOwnedBySystemDeviceType(JblParkNode node, String eventName, CounterType counterType, JblParkCounterSourceFilterSet filterSet) {
        List<DeviceType> result = new ArrayList<>();

        if (!node.isDevice()) {
            if (NodeType.PARKING.equals(node.getNodeType())) {
                if (JpsOpUsrTransit.class.getSimpleName().equals(eventName)) {
                    if (CounterType.PER_PRODUCT_PER_TRANSIENT.equals(counterType) &&
                            (JpsCountingSourceFilterSetType.BOOKING_TRANSIT.equals(filterSet.getOwnedBySystemFilterSetType()) ||
                                    JpsCountingSourceFilterSetType.BOOKING_TRANSIT_REVERSE.equals(filterSet.getOwnedBySystemFilterSetType()))) {
                        // EBBS-6307 assign booking transit source to transient counter only at entry stations
                        result.add(DeviceType.ENTRY_STATION);
                    } else {
                        result.addAll(Arrays.asList(DeviceType.ENTRY_STATION, DeviceType.EXIT_STATION));
                    }
                }
                else
                    result.add(null);
            } else if (NodeType.AREA.equals(node.getNodeType())) {
                if (!JpsOpUsrTransit.class.getSimpleName().equals(eventName))
                    result.add(null);
            }

        } else {
            JblParkNode parentNode = parkNodes.stream().filter(pn -> Long.valueOf(pn.getId()).equals(node.getParentId())).findFirst().orElse(null);

            if (parentNode != null && NodeType.AREA.equals(parentNode.getNodeType()))
                result.add(DeviceType.LANE_SECTION);
        }

        return result;
    }

    private void getLinkInputTypeBySource(JblParkCounterSource source, CounterType counterType, Handler<AsyncResult<JpsCountingLinkInputType>> resultHandler) {
        if (JpsOpUsrTransit.class.getSimpleName().equals(source.getSourceFilterSet().getEventName())) {
            JpsCountingLinkInputType inputType = JpsCountingLinkInputType.NotSpecified;

            if (isPeripheralTypeEqualSourceDeviceType(PeripheralsHex.LE, source))
                inputType = CounterIncOffset;
            else if (isPeripheralTypeEqualSourceDeviceType(PeripheralsHex.LX, source))
                inputType = CounterDecOffset;
            else if (isPeripheralTypeEqualSourceDeviceType(PeripheralsHex.LS, source)) {
                JblParkNode node = parkNodes.stream().filter(pn -> source.getPeripheralNodeId() != null && Long.valueOf(pn.getId()).equals(source.getPeripheralNodeId())).findFirst().orElse(null);
                if (node != null) {
                    if (node.getToId() == null)
                        inputType = CounterDecOffset;
                    else if (node.getFromId() == null)
                        inputType = CounterIncOffset;
                    else {
                        JblParkNode targetNode = parkNodes.stream().filter(pn -> pn.getId() == node.getToId()).findFirst().orElse(null);

                        if (targetNode != null) {
                            if (NodeType.AREA.equals(targetNode.getNodeType()))
                                inputType = CounterIncOffset;
                            else
                                inputType = CounterDecOffset;
                        }
                    }
                }
            }

            if (inputType != NotSpecified) {
                if (CounterType.PER_PRODUCT_PER_TRANSIENT.equals(counterType) &&
                        (JpsCountingSourceFilterSetType.BOOKING_TRANSIT.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()) ||
                                JpsCountingSourceFilterSetType.BOOKING_TRANSIT_REVERSE.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))) {
                    // EBBS-6307 decrement transient counter in case of booking transits
                    if (JpsCountingSourceFilterSetType.BOOKING_TRANSIT.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                        inputType = CounterDecOffset;
                    else
                        inputType = CounterIncOffset;
                } else if (source.getSourceFilterSet().isTransitReverse()) {
                    if (inputType == CounterIncOffset)
                        inputType = CounterDecOffset;
                    else
                        inputType = CounterIncOffset;
                }
            }

            resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(inputType).build());
        } else if (JpsOpUsrPayCompl.class.getSimpleName().equals(source.getSourceFilterSet().getEventName())) {
            if (JpsCountingSourceFilterSetType.CONVERT_TRANSIENT_TO_ANONYMOUS_SUBSCRIPTION.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(CounterType.PER_PRODUCT_TIME_BASED_ANONYMOUS.equals(counterType) ? CounterIncOffset : CounterDecOffset).build());
            else
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(NotSpecified).build());
        } else if (JpsLogFloorCounter.class.getSimpleName().equals(source.getSourceFilterSet().getEventName())) {
            if (JpsCountingSourceFilterSetType.FLOOR_COUNTER_IN.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(CounterIncOffset).build());
            else if (JpsCountingSourceFilterSetType.FLOOR_COUNTER_OUT.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(CounterDecOffset).build());
            else
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(NotSpecified).build());
        } else if (JblInternalCountingEvent.class.getSimpleName().equals(source.getSourceFilterSet().getEventName())) {
            if (JpsCountingSourceFilterSetType.BOOKING_RESERVE_PLACE.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(CounterIncOffset).build());
            else if (JpsCountingSourceFilterSetType.BOOKING_FREE_PLACE.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(CounterDecOffset).build());
            else if (JpsCountingSourceFilterSetType.PLATE_RESET_TRANSIENT_ENTRANCE.equals(source.getSourceFilterSet().getOwnedBySystemFilterSetType()))
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(CounterDecOffset).build());
            else
                resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(NotSpecified).build());
        } else
            resultHandler.handle(new AsyncResultBuilder<JpsCountingLinkInputType>().withSuccess().withResult(NotSpecified).build());
    }

    private String getMainPeripheralTypeByDeviceType(DeviceType deviceType) {
        switch (deviceType) {
            case ENTRY_STATION:
            case ENTRY_PAYMENT_STATION:
            case VRT_ENTRY_STATION:
                return PeripheralsHex.LE;
            case EXIT_STATION:
            case VRT_EXIT_STATION:
            case FCJ_ON_LANE:
                return PeripheralsHex.LX;
            case LANE_SECTION:
            case VRT_SECTION_STATION:
                return PeripheralsHex.LS;
            case PAYMENT_STATION:
            case HANDHELD_CASHIER:
            case JANUS_FEE_COMPUTER:
                return PeripheralsHex.APS;
            case DOOR_READER:
                return PeripheralsHex.DR;
            default:
                return null;
        }
    }

    private boolean isPeripheralTypeEqualSourceDeviceType(String peripheralType, JblParkCounterSource source) {
        return isPeripheralTypeEqualSourceDeviceType(JpsPeripheral.newInstance(peripheralType), source);
    }

    private boolean isPeripheralTypeEqualSourceDeviceType(JpsPeripheral peripheral, JblParkCounterSource source) {
        Long sourceNodeId = Objects.requireNonNullElse(source.getPeripheralNodeId(), source.getNodeId());
        JblParkNode sourceNode = parkNodes.stream().filter(pn -> pn.getId() == sourceNodeId.longValue()).findFirst().orElse(null);

        if (sourceNode != null) {
            if (!sourceNode.isDevice())
                return isPeripheralTypeEqualSystemDeviceFilter(peripheral, source.getDeviceType());
            else if (peripheral != null && peripheral.getPeripheralType() != null)
                return peripheral.getPeripheralType().equals(getMainPeripheralTypeByDeviceType(sourceNode.getDeviceType()));
        }

        return false;
    }

    private boolean isPeripheralTypeEqualSystemDeviceFilter(JpsPeripheral peripheral, DeviceType deviceFilter) {
        if (deviceFilter == null)
            return true;

        if (peripheral != null) {
            switch (deviceFilter) {
                case ENTRY_STATION:
                    return peripheral.isEntry() && !peripheral.isDoor(); // EBBS-7821
                case EXIT_STATION:
                    return peripheral.isExit();
                case LANE_SECTION:
                    return peripheral.isPassage();
                case PAYMENT_STATION:
                    return peripheral.isPayment();
                case DOOR_READER:
                    return peripheral.isDoor();
            }
        }

        return false;
    }

    private String buildCounterSourceName(JblParkCounterSource source, JblParkNode node) {
        String sourceName = node.getName() + " - ";

        if (source.getSourceFilterSet().getEventName().equals(JpsOpUsrTransit.class.getSimpleName())) {
            if (isPeripheralTypeEqualSourceDeviceType(PeripheralsHex.LE, source))
                return sourceName + source.getSourceFilterSet().getName().replace("transit", "entry");
            else if (isPeripheralTypeEqualSourceDeviceType(PeripheralsHex.LX, source))
                return sourceName + source.getSourceFilterSet().getName().replace("transit", "exit");
            else if (isPeripheralTypeEqualSourceDeviceType(PeripheralsHex.LS, source))
                return sourceName + source.getSourceFilterSet().getName().replace("transit", "passage");
        }

        return sourceName + source.getSourceFilterSet().getName();
    }

    //region PROXY implementations
    @Override
    public void onCountingEvent(JsonObject countingEvent, Handler<AsyncResult<Void>> resultHandler) {
        JblInternalCountingEvent evt = countingEvent.mapTo(JblInternalCountingEvent.class);
        JblEventExtendedJbl jblEvent = new JblEventExtendedJbl();

        jblEvent.setPeripheralId(evt.getPeripheralId());
        jblEvent.setEventSpecCode(evt.getJpsEventSpecCode());
        jblEvent.setEventType(evt.getJpsEventType());
        jblEvent.setJson(evt.getJpsEventJson());

        processCountingSources(jblEvent, evt, resultHandler);
    }

    public void createCounterSources(SQLConnectionWrapper conn, CounterType counterType, Long nodeId, Long peripheralNodeId, Handler<AsyncResult<String>> resultHandler) {
        final List<CounterSourceLinkDTO> result = new ArrayList<>();
        final List<JblParkCounterSource> insertedSources = new ArrayList<>();
        final JblParkNode node = parkNodes.stream().filter(pn -> pn.getId() == Objects.requireNonNullElse(peripheralNodeId, nodeId)).findFirst().orElse(null);

        if (node != null) {
            final List<JblParkCounterSourceFilterSet> sourceFilterSets = new ArrayList<>();

            if (NodeType.PARKING.equals(node.getNodeType()))
                sourceFilterSets.addAll(filterSets.stream().filter(fs -> fs.isOwnedBySystem() && fs.getCounterTypes().contains(counterType) && (fs.getNodeTypeLimits().isEmpty() || fs.getNodeTypeLimits().contains(node.getNodeType()))).collect(Collectors.toList()));
            else if (NodeType.AREA.equals(node.getNodeType())) {
                // process area's non transit filter sets
                sourceFilterSets.addAll(filterSets.stream().filter(fs -> !fs.getEventName().equals(JpsOpUsrTransit.class.getSimpleName()) && fs.isOwnedBySystem() && fs.getCounterTypes().contains(counterType) && (fs.getNodeTypeLimits().isEmpty() || fs.getNodeTypeLimits().contains(node.getNodeType()))).collect(Collectors.toList()));
            } else if (node.getDeviceType() != null) {
                JblParkNode parentNode = parkNodes.stream().filter(pn -> Long.valueOf(pn.getId()).equals(node.getParentId())).findFirst().orElse(null);

                if (parentNode != null && NodeType.AREA.equals(parentNode.getNodeType())) {
                    // process area's transit filter sets on device level to determine transit direction
                    sourceFilterSets.addAll(filterSets.stream().filter(fs -> fs.getEventName().equals(JpsOpUsrTransit.class.getSimpleName()) && fs.isOwnedBySystem() && fs.getCounterTypes().contains(counterType)).collect(Collectors.toList()));
                }
            } else {
                resultHandler.handle(Future.failedFuture(""));
                return;
            }

            if (!sourceFilterSets.isEmpty() || NodeType.AREA.equals(node.getNodeType())) {
                List<Promise> filterSetPromises = sourceFilterSets.stream().map(fs -> Promise.promise()).collect(Collectors.toList());

                CompositeFuture.all(PromiseUtil.toFutureList(filterSetPromises)).onComplete(compositeFutureAsyncResult -> {
                    if (compositeFutureAsyncResult.succeeded()) {
                        List<Promise> areaPromises = new ArrayList<>();

                        if (NodeType.AREA.equals(node.getNodeType())) {
                            // process area's device node transit filter sets
                            List<JblParkNode> areaDevices = parkNodes.stream().filter(pn -> Long.valueOf(node.getId()).equals(pn.getToId()) || Long.valueOf(node.getId()).equals(pn.getFromId())).collect(Collectors.toList());
                            areaPromises.addAll(areaDevices.stream().map(ad -> Promise.promise()).collect(Collectors.toList()));

                            int i = 0;
                            for (JblParkNode areaDevice : areaDevices) {
                                final int areaDeviceIndex = i;
                                createCounterSources(conn, counterType, nodeId, areaDevice.getId(), areaDeviceResult -> {
                                    if (areaDeviceResult.succeeded()) {
                                        result.addAll(JsonUtilsExtended.deserializeWithType(areaDeviceResult.result(), new TypeReference<List<CounterSourceLinkDTO>>() {
                                        }));
                                        areaPromises.get(areaDeviceIndex).complete(Future.succeededFuture());
                                    } else {
                                        areaPromises.get(areaDeviceIndex).complete(Future.failedFuture(areaDeviceResult.cause()));
                                        context.getLogger(getClass()).error(areaDeviceResult.cause());
                                    }
                                });
                                i++;
                            }
                        }

                        CompositeFuture.all(PromiseUtil.toFutureList(areaPromises)).onComplete(areaDevicesResult -> {
                            if (areaDevicesResult.succeeded()) {
                                Map<String, List<JblParkCounterSource>> sourcesByEventName = insertedSources.stream().collect(Collectors.groupingBy(x -> x.getSourceFilterSet().getEventName().trim().toLowerCase()));

                                for (Map.Entry<String, List<JblParkCounterSource>> eventSources : sourcesByEventName.entrySet()) {
                                    if (!counterSources.containsKey(eventSources.getKey()))
                                        counterSources.put(eventSources.getKey(), eventSources.getValue());
                                    else
                                        counterSources.get(eventSources.getKey()).addAll(eventSources.getValue());
                                }

                                String json = JSONUtil.serializeWithType(result, new TypeReference<List<CounterSourceLinkDTO>>() {
                                });
                                resultHandler.handle(Future.succeededFuture(json));
                            } else {
                                resultHandler.handle(new AsyncResultBuilder<String>().withFail().build());
                            }
                        });
                    } else {
                        resultHandler.handle(new AsyncResultBuilder<String>().withFail().build());
                    }

                    context.close();
                });

                int i = 0;

                // create counting source(s) for each filter set
                for (JblParkCounterSourceFilterSet fs : sourceFilterSets) {
                    final int filterSetPromiseIndex = i;
                    final String eventName = fs.getEventName().trim().toLowerCase();

                    if (!counterSources.containsKey(eventName))
                        counterSources.put(eventName, new ArrayList<>());

                    List<Tuple<DeviceType, Promise>> deviceTypes = getOwnedBySystemDeviceType(node, fs.getEventName().trim(), counterType, fs).stream().map(dt -> new Tuple<DeviceType, Promise>(dt, Promise.promise())).collect(Collectors.toList());

                    CompositeFuture.all(PromiseUtil.toFutureList(deviceTypes.stream().map(Tuple::getSecond).collect(Collectors.toList()))).onComplete(asyncDeviceTypesResult -> {
                        if (asyncDeviceTypesResult.succeeded())
                            filterSetPromises.get(filterSetPromiseIndex).complete();
                        else {
                            context.getLogger(getClass()).error(asyncDeviceTypesResult.cause());
                            filterSetPromises.get(filterSetPromiseIndex).fail(asyncDeviceTypesResult.cause());
                        }
                    });

                    for (Tuple<DeviceType, Promise> deviceType : deviceTypes) {
                        Stream<JblParkCounterSource> streamFilter = counterSources.get(eventName).stream().filter(s -> s.getSourceFilterSetId().equals(fs.getId()));

                        if (peripheralNodeId != null)
                            streamFilter = streamFilter.filter(s -> peripheralNodeId.equals(s.getPeripheralNodeId()));
                        else
                            streamFilter = streamFilter.filter(s -> nodeId.equals(s.getNodeId()) && ((deviceType.getFirst() != null && deviceType.getFirst().equals(s.getDeviceType())) || (deviceType.getFirst() == null && s.getDeviceType() == null)));

                        JblParkCounterSource counterSource = streamFilter.findFirst().orElse(null);

                        if (counterSource == null) {
                            counterSource = new JblParkCounterSource();

                            if (peripheralNodeId != null)
                                counterSource.setPeripheralNodeId(peripheralNodeId);
                            else
                                counterSource.setNodeId(nodeId);

                            counterSource.setShortName("");
                            counterSource.setEnabled(true);
                            counterSource.setDeviceType(deviceType.getFirst());
                            counterSource.setSourceFilterSetId(fs.getId());
                            counterSource.setSourceFilterSet(fs);
                            counterSource.setName(StringUtils.substring(buildCounterSourceName(counterSource, node), 0, 100));
                            counterSource.setRemarks("created by JMS operator");
                            counterSource.setOwnedBySystem(true);

                            jblParkCounterSourceDao.insert(context, conn, counterSource, insertResult -> {
                                if (insertResult.succeeded()) {
                                    insertedSources.add(insertResult.result());

                                    getLinkInputTypeBySource(insertResult.result(), counterType, inputType -> {
                                        if (!inputType.result().equals(NotSpecified))
                                            result.add(new CounterSourceLinkDTO(insertResult.result().getId(), inputType.result()));

                                        deviceType.getSecond().complete();
                                    });
                                } else {
                                    context.getLogger(getClass()).error(insertResult.cause());
                                    deviceType.getSecond().fail(insertResult.cause());
                                }
                            });
                        } else {
                            final long counterSourceId = counterSource.getId();
                            final JblParkCounterSource cs = counterSource;

                            getLinkInputTypeBySource(counterSource, counterType, inputType -> {
                                if (!inputType.result().equals(NotSpecified))
                                    result.add(new CounterSourceLinkDTO(counterSourceId, inputType.result()));

                                deviceType.getSecond().complete();
                            });
                        }
                    }
                    i++;
                }
            } else {
                resultHandler.handle(Future.succeededFuture(JSONUtil.serializeWithType(result, new TypeReference<List<CounterSourceLinkDTO>>() {
                })));
            }
        } else {
            resultHandler.handle(new AsyncResultBuilder<String>().withFail().withCause(new Exception("unknown node Id " + Objects.requireNonNullElse(peripheralNodeId, nodeId))).build());
        }
    }
}
