package hub.ebb.jblcluster.eventservice.service.impl;

import com.google.common.base.Strings;
import hub.jbl.common.lib.api.customer.CustomerAPI;
import hub.jbl.common.lib.api.productprofile.ProductAPI;
import hub.jbl.common.lib.api.validation.DiscountOnPaymentTypeAPI;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.date.DateUtils;
import hub.jbl.common.lib.number.MoneyUtils;
import hub.jbl.common.lib.utils.date.SubscriptionRenewCalculator;
import hub.jbl.common.lib.utils.handler.JblHandler;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.core.dto.jps.cardValidation.JpsSellableProductDTO;
import hub.jbl.core.dto.jps.event.JpsUsrPassMediaType;
import hub.jbl.dao.MembershipDao;
import hub.jbl.dao.MembershipMediaTypesDao;
import hub.jbl.dao.ProductProfileDao;
import hub.jbl.dao.TransientUsrPassDao;
import hub.jbl.dao.common.EntityDoNotExistsException;
import hub.jbl.dao.util.SQLConnectionWrapper;
import hub.jbl.entity.cardvalidation.JblTransientUsrPass;
import hub.jbl.entity.membership.JblMembership;
import hub.jbl.entity.productProfile.JblProductProfile;
import hub.ebb.jblcluster.eventservice.model.SellableProductType;
import hub.jbl.common.applybypaymenttype.CalculationOfDiscountOnPaymentType;
import hub.ebb.jblcluster.eventservice.service.JpsContractIssuingService;
import hub.jms.common.model.product.profile.ProductProfileType;
import hub.jms.common.model.product.profile.complextype.DurationEndingType;
import hub.jms.common.model.product.profile.complextype.DurationStartingType;
import hub.jms.common.model.product.profile.complextype.TimeUnit;
import hub.jms.common.model.utils.JSONUtil;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JpsContractIssuingServiceImpl implements JpsContractIssuingService {

    @Autowired
    private JblTransactionManager transactionManager;
    @Autowired
    private ProductProfileDao productProfileDao;
    @Autowired
    private TransientUsrPassDao transientUsrPassDao;
    @Autowired
    private ProductAPI productAPI;
    @Autowired
    private CustomerAPI customerAPI;
    @Autowired
    private MembershipDao membershipDao;
    @Autowired
    private MembershipMediaTypesDao membershipMediaTypesDao;
    @Autowired
    private DiscountOnPaymentTypeAPI discountOnPaymentTypeAPI;


    @Override
    public void getSellableProducts(JBLContext context, SellableProductType productType, String identifier, Handler<AsyncResult<JsonArray>> asyncResultHandler) {

        transactionManager.executeTransaction((conn, transactionBodyResultHandler) -> {
            JblProductProfile example = new JblProductProfile();
            switch (productType) {
                default:
                case APS:
                    example.setSellableFromAps(true);
                    break;
                case FCJ:
                case FCJ_OL:
                    example.setSellableFromFcj(true);
                    break;
                case LE_POS:
                case LE:
                    example.setSellableFromLe(true);
                    break;
            }
            productProfileDao.findByExample(context, conn, example).thenAccept(listAsyncResult -> {
                getTicketData(context, conn, identifier, asyncResultTicketData -> {
                    transactionBodyResultHandler.handle(Future.succeededFuture());
                    if (listAsyncResult.succeeded()) {
                        List<JblProductProfile> productProfiles = listAsyncResult.result();
                        if (productProfiles.isEmpty()) {
                            asyncResultHandler.handle(Future.succeededFuture(new JsonArray()));
                        } else {
                            JsonArray jsonArray = new JsonArray();
                            AtomicInteger i = new AtomicInteger();
                            Promise<Object> promise = Promise.promise();

                            promise.future().onSuccess(handler -> {
                                try{
                                    asyncResultHandler.handle(Future.succeededFuture(getSortedJsonArrayByName(jsonArray)));
                                }
                                catch (RuntimeException e){
                                    context.getLogger(this.getClass()).error("Error in sorting Anonymous subscription product profiles");
                                    e.printStackTrace();
                                    asyncResultHandler.handle(Future.succeededFuture(jsonArray));
                                }
                                
                            });

                            productProfiles.stream().sorted(Comparator.comparing(JblProductProfile::getSalePrice)).forEach(prod -> {
                                JsonObject obj = new JsonObject();
                                obj.put("uid", prod.getIdentifier());
                                obj.put("name", prod.getName());
                                obj.put("amount", MoneyUtils.getMoney(prod.getSalePrice()).doubleValue());
                                obj.put("prdType", prod.getProductProfileType());

                                Long startValidityTs = DateUtils.getUnixTSInMillis();

                                if (DurationStartingType.TICKET_ENTRY_TIME.name().equals(prod.getDurationStartType()) && asyncResultTicketData.succeeded() && asyncResultTicketData.result() != null) {
                                    // Duration Stating type is Ticket entry time
                                    startValidityTs = asyncResultTicketData.result().getIssueDateTime().getTimestamp();
                                    obj.put("startValidityTs", startValidityTs);
                                }
                                obj.put("endValidityTs", SubscriptionRenewCalculator.calculateRenewFromStartValidity(startValidityTs, startValidityTs, TimeUnit.valueOf(prod.getTimeUnit()), prod.getTimeValue(), DurationEndingType.valueOf(prod.getDurationEndType())));

                                if (ProductProfileType.TIME_BASED_ANONYMOUS.name().equals(prod.getProductProfileType())) {
                                    // for Anonymous usage number of days
                                    obj.put("enAnonNumOfDays", prod.isEnableAnonymousUsageNumberOfDays());
                                    obj.put("anonNumOfDays", prod.getAnonymousUsageNumberOfDays());
                                }

                                JpsSellableProductDTO jpsSellableProduct = new JpsSellableProductDTO();
                                jpsSellableProduct.setAmount(BigDecimal.valueOf(MoneyUtils.getMoney(prod.getSalePrice()).doubleValue()));
                                jpsSellableProduct.setStartValidityTs(startValidityTs);
                                jpsSellableProduct.setEndValidityTs(obj.getLong("endValidityTs"));

                                JsonObject jpsSellableProductJson = new JsonObject(JSONUtil.serialize(jpsSellableProduct));
                                discountOnPaymentTypeAPI.calculationBasedOnPaymentTypes(null, jpsSellableProductJson, calculatedResult -> {
                                    if (calculatedResult.succeeded() && calculatedResult.result() != null) {
                                        i.incrementAndGet();
                                        obj.mergeIn(calculatedResult.result());
                                        jsonArray.add(obj);
                                    }

                                    if (i.get() == productProfiles.size()) {
                                        promise.complete();
                                    }
                                });
                            });
                        }
                    } else {
                        asyncResultHandler.handle(Future.failedFuture(listAsyncResult.cause()));
                    }
                });
            });
        });
    }

    @Override
    public void getSellableProductsForMembership(JBLContext context, String membershipUuid, String identifier, JpsUsrPassMediaType mediaType, JblHandler<JsonArray> resultHandler) {
        transactionManager.executeTransaction(((conn, transactionBodyResultHandler) -> {
            JblMembership memEx = new JblMembership();
            memEx.setUuid(membershipUuid);
            membershipDao.findOneByExample(context, conn, memEx).thenAccept(findMembershipAsyncResult -> {
                if (findMembershipAsyncResult.succeeded()) {
                    if (findMembershipAsyncResult.result() == null) {
                        transactionBodyResultHandler.handle(Future.succeededFuture());
                        resultHandler.handle(Future.failedFuture(new EntityDoNotExistsException()));
                    } else {
                        var membership = findMembershipAsyncResult.result();
                        this.productProfileDao.findById(context, conn, membership.getRegistrationProductId()).thenAccept(getMembershipProductProfileAsyncResult -> {
                            if (getMembershipProductProfileAsyncResult.succeeded()) {
                                if (getMembershipProductProfileAsyncResult.result() == null) {
                                    transactionBodyResultHandler.handle(Future.succeededFuture());
                                    resultHandler.handle(Future.failedFuture(new EntityDoNotExistsException()));
                                } else {
                                    this.productAPI.isMembershipContractReplaceAllowed_API(getMembershipProductProfileAsyncResult.result().getId(), replaceAllowedAsyncResult -> {
                                        if (replaceAllowedAsyncResult.succeeded()) {
                                            if (Boolean.TRUE.equals(replaceAllowedAsyncResult.result())) {
                                                this.membershipMediaTypesDao.getPlateForF2Filter(context, conn, mediaType, membership.getId(), platesAsyncResult -> {
                                                    transactionBodyResultHandler.handle(Future.succeededFuture());
                                                    String plateForF2;
                                                    if (platesAsyncResult.succeeded()) {
                                                        if (platesAsyncResult.result() == null) {
                                                            //in this case identifier = plate
                                                            plateForF2 = identifier;
                                                        } else if (platesAsyncResult.result().isEmpty()) {
                                                            //in this case no vehicle owned by customer --> f2 filter cannot be applied
                                                            plateForF2 = null;
                                                        } else {
                                                            //in this case i use the first registered plate for F2 filtering --> all vehicles must belong to same VCS
                                                            plateForF2 = platesAsyncResult.result().get(0);
                                                        }
                                                        final String plateForF2Copy = plateForF2;
                                                        this.lockPlateForIssuingIfNecessary(context, identifier, lockPlateAsyncResult -> {
                                                            if (lockPlateAsyncResult.succeeded()) {
                                                                if (lockPlateAsyncResult.result().equals(Boolean.FALSE)) {
                                                                    //if plate is already locked then return an empty list
                                                                    JsonArray result = new JsonArray();
                                                                    resultHandler.handle(Future.succeededFuture(result));
                                                                } else {
                                                                    try {
                                                                        this.productAPI.retrieveMembershipSellableProducts_API(getMembershipProductProfileAsyncResult.result().getId(), plateForF2Copy == null ? new ArrayList<>() : List.of(plateForF2Copy), getSellableProductsAsyncResult -> {
                                                                            if (getSellableProductsAsyncResult.succeeded()) {
                                                                                JsonArray sellableProductsRaw = getSellableProductsAsyncResult.result().getJsonArray("products");
                                                                                List<JpsSellableProductDTO> array = new ArrayList<>();
                                                                                JsonArray result = new JsonArray();
                                                                                for (int i = 0; i < sellableProductsRaw.size(); i++) {
                                                                                    JblProductProfile prod = JSONUtil.deserialize(sellableProductsRaw.getJsonObject(i).encode(), JblProductProfile.class);
                                                                                    JpsSellableProductDTO tmp = new JpsSellableProductDTO();
                                                                                    tmp.setUid(prod.getIdentifier());
                                                                                    tmp.setUuid(prod.getUuid());
                                                                                    tmp.setName(prod.getName());
                                                                                    tmp.setPrdType(prod.getProductProfileType());
                                                                                    tmp.setAmount(prod.getSalePrice() != null ? prod.getSalePrice() : new BigDecimal(0));
                                                                                    tmp.setStartType(prod.getDurationStartType());
                                                                                    Long startValidityTs = hub.jbl.common.utils.date.SubscriptionRenewCalculator.calculateStartValidityForSellableLTSProduct(prod);
                                                                                    tmp.setStartValidityTs(startValidityTs);
                                                                                    tmp.setEndValidityTs(hub.jbl.common.utils.date.SubscriptionRenewCalculator.calculateEndValidityForSellableLTSProduct(prod, startValidityTs, false));
                                                                                    array.add(tmp);
                                                                                    result.add(new JsonObject(JSONUtil.serialize(tmp)));
                                                                                }
                                                                                resultHandler.handle(Future.succeededFuture(result));
                                                                            } else {
                                                                                this.unlockPlateForIssuingIfNecessary(context, identifier, unlockPlate -> {
                                                                                });
                                                                                resultHandler.handle(Future.failedFuture(getMembershipProductProfileAsyncResult.cause()));
                                                                            }
                                                                        });
                                                                    } catch (Exception e) {
                                                                        this.unlockPlateForIssuingIfNecessary(context, identifier, unlockPlate -> {
                                                                        });
                                                                        resultHandler.handle(Future.failedFuture(e));
                                                                    }
                                                                }
                                                            } else {
                                                                resultHandler.handle(Future.failedFuture(lockPlateAsyncResult.cause()));
                                                            }
                                                        });
                                                    } else {
                                                        resultHandler.handle(Future.failedFuture(platesAsyncResult.cause()));
                                                    }
                                                });
                                            } else {
                                                //if membership product profile does not allow replace of contract then return an empty list
                                                JsonArray result = new JsonArray();
                                                transactionBodyResultHandler.handle(Future.succeededFuture());
                                                resultHandler.handle(Future.succeededFuture(result));
                                            }
                                        } else {
                                            transactionBodyResultHandler.handle(Future.succeededFuture());
                                            resultHandler.handle(Future.failedFuture(replaceAllowedAsyncResult.cause()));
                                        }
                                    });
                                }
                            } else {
                                transactionBodyResultHandler.handle(Future.succeededFuture());
                                resultHandler.handle(Future.failedFuture(getMembershipProductProfileAsyncResult.cause()));
                            }
                        });
                    }
                } else {
                    transactionBodyResultHandler.handle(Future.succeededFuture());
                    resultHandler.handle(Future.failedFuture(findMembershipAsyncResult.cause()));
                }
            });
        }));
    }

    public void getTicketData(JBLContext context, SQLConnectionWrapper connection, String identifier, Handler<AsyncResult<JblTransientUsrPass>> asyncResultHandler) {
        if (!Strings.isNullOrEmpty(identifier)) {
            transientUsrPassDao.getTransientByTicketIdentifierOrRawdata(context, connection, identifier, transientUsrPassAsyncResult -> {
                if (transientUsrPassAsyncResult.succeeded()) {
                    asyncResultHandler.handle(Future.succeededFuture(transientUsrPassAsyncResult.result()));
                } else {
                    asyncResultHandler.handle(Future.failedFuture(transientUsrPassAsyncResult.cause()));
                }
            });
        } else {
            asyncResultHandler.handle(Future.succeededFuture());
        }
    }

    private void lockPlateForIssuingIfNecessary(JBLContext context, String licensePlate, JblHandler<Boolean> resultHandler) {
        if (StringUtils.isNotBlank(licensePlate)) {
            this.customerAPI.managePlateLockingForIssuing(licensePlate, lockPlateAsyncResult -> {
                if (lockPlateAsyncResult.succeeded()) {
                    if (lockPlateAsyncResult.result() == null || lockPlateAsyncResult.result().equals(Boolean.TRUE)) {
                        resultHandler.handle(Future.succeededFuture(true));
                    } else {
                        context.getLogger(this.getClass()).error("lockPlateForIssuingIfNecessary found that plate " + licensePlate + "is already locked for issuing");
                        resultHandler.handle(Future.succeededFuture(false));
                    }
                } else {
                    context.getLogger(this.getClass()).error("lockPlateForIssuingIfNecessary was unable to lock plate " + licensePlate + "for issuing", lockPlateAsyncResult.cause());
                    resultHandler.handle(Future.failedFuture(lockPlateAsyncResult.cause()));
                }
            });
        } else {
            resultHandler.handle(Future.succeededFuture(true));
        }
    }

    private void unlockPlateForIssuingIfNecessary(JBLContext context, String licensePlate, JblHandler<Void> resultHandler) {
        if (StringUtils.isNotBlank(licensePlate)) {
            this.customerAPI.unlockPlateForIssuing(licensePlate, unlockPlateAsyncResult -> {
                if (unlockPlateAsyncResult.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    context.getLogger(this.getClass()).error("unlockPlateForIssuingIfNecessary was unable to unlock plate " + licensePlate + "for issuing", unlockPlateAsyncResult.cause());
                    resultHandler.handle(Future.failedFuture(unlockPlateAsyncResult.cause()));
                }
            });
        } else {
            resultHandler.handle(Future.succeededFuture());
        }
    }
    
    private JsonArray getSortedJsonArrayByName(JsonArray array) throws RuntimeException {
        array.getList();
        ArrayList<JsonObject> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add(array.getJsonObject(i));
        }
        list.sort((obj1, obj2) -> {
            try {
                return obj1.getString("name").compareToIgnoreCase(obj2.getString("name"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        });
        return new JsonArray(list);
    }
}
