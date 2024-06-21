package hub.ebb.jblcluster.eventservice.model;

import hub.ebb.jblcluster.eventservice.mapper.JblEventMapperVisitor;
import hub.ebb.jblcluster.eventservice.mapper.JblVisitableEvent;
import hub.ebb.jblcluster.eventservice.mapper.JmsEvent;
import hub.jbl.common.lib.builder.AsyncResultBuilder;
import hub.jbl.core.dto.jps.cardValidation.CalcType;
import hub.jbl.core.dto.jps.cardValidation.CarLocationDataDTO;
import hub.jbl.core.dto.jps.cardValidation.JpsAdditionalServiceDataDTO;
import hub.jbl.core.dto.jps.cardValidation.JpsRetCode;
import hub.jbl.core.dto.jps.event.JpsLogUsrPass;
import hub.jbl.core.dto.jps.event.JpsUsrOperation;
import hub.jbl.core.dto.jps.event.JpsUsrPassType;
import hub.jbl.core.visitor.jps.JmsCommonEvent;
import hub.jbl.core.visitor.jps.JpsEventMapperVisitor;
import hub.jbl.entity.cardvalidation.JblTransientUsrPass;
import hub.jbl.entity.productProfile.JblProductProfile;
import hub.jms.common.model.eventParking.EventParkingPrepayType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JpsUsrCardValidationResult extends JpsUsrOperation implements JblVisitableEvent {


    private static final long serialVersionUID = -2497897492648488567L;
    private List<Overage> overages;
    private JpsRetCode retCode;
    private List<JpsUsrCardValidationWarningData> warningsData;
    private CarLocationDataDTO carLocationData;
    private BigDecimal amount = BigDecimal.ZERO;
    private int freeTimeLeft = 0;
    private long timeToExit = 0;
    private String message;
    private Long endValidityTs;
    private Long endRenewValidityTs;
    private Long startValidityTs;
    private String contractNumber;
    private String cardNumber;
    private JpsUsrPassType type;
    private String prodProfile;
    private String prodProfileType;
    private BigDecimal deductedAmount = BigDecimal.ZERO;
    private long parkingTime = 0;
    private long payDiffTime = 0;
    private Long entryTs;
    private Long payTs;
    private String licensePlate;
    private String cardPoolName;
    private Integer cardPoolActualValue;
    private String cardPoolType;
    private Long prepaymentExitTime;
    private boolean overageForcePaymentRenewal = false;
    private String pin;
    private String username;
    private Integer rempinattempts;
    private Integer remDailyUsg;
    private Integer remTotalUsg;
    private String accountName;
    private boolean reserved;
    private String status;
    private BigDecimal discountedAmount;
    private String bayNumber;
    private String tagTelepass;
    private String uniqueSaleNumber;
    private String description;
    private String ucName;
    private BigDecimal ucAmount = BigDecimal.ZERO;
    private List<JblProductProfile> sellableProducts;
    private int earlyArrivalAllowed;
    private int laterDepartureAllowed;
    private String ucValue;
    private String ucClass;
    private boolean enAnonNumOfDaysUsg;
    private Integer remAnonNumOfDaysUsg;
    private long previousStayInMinutes;
    private boolean proratedAmount;
    private List<String> receiptTexts;
    private Integer receiptCopies;
    private JblTransientUsrPass transientUsrPass;
    private Integer freeOfChargeTimeLeft;
    private String operatorCardUsageType;
    private String evtParkingDisplayMsg;
    private BigDecimal evtParkingFlatFee = BigDecimal.ZERO;
    private Long evtParkingPrepaidTime;
    private EventParkingPrepayType evtParkingPrepayType;
    private Long evtParkingPrepaidDays;
    private String evtParkingPrepaidHHmm;
    private Long evtParkingPrepayUC;
    private String gpFacilityName;
    private String gpRoomNumber;
    private String gpFirstName;
    private String gpLastName;
    private String customPointsLabel;
    private Boolean validatedOnAps;
    private BigDecimal posDiscountedAmount = BigDecimal.ZERO;
    private String posDiscDescr;
    private BigDecimal cashDiscountedAmount = BigDecimal.ZERO;
    private String cashDiscDescr;
    private BigDecimal provsDiscountedAmount = BigDecimal.ZERO;
    private String provsDiscDescr;
    private String sourceCode;
    private String oldLstUid;
    private CalcType calcType;
    private Double pricePerPoint;
    private Map<Double, Short> pointsAmountForQty;
    private Boolean jmsOfflineStatus;
    private boolean lowBalance;
    private boolean isMembershipExpired;
    private String membershipUuid;
    private BigDecimal taxAmountBWI;
    private Integer taxableNumberOfDaysBWI;
    private JpsAdditionalServiceDataDTO additionalServices;
    private Boolean prodProfileEnablePostPayment;
    private Boolean payWithJp;

    @Override
    public CompletableFuture<AsyncResult<JmsEvent>> accept(JblEventMapperVisitor visitor) {
        CompletableFuture<AsyncResult<JmsEvent>> result = new CompletableFuture<>();
        try {
            result = visitor.map(this);
        } catch (Exception e) {
            hub.jbl.common.lib.context.JBLContext.getInstance().getLogger(getClass()).error("Error on mapping " + getClass().getName() + " event.", e);
            result.complete(new AsyncResultBuilder<JmsEvent>().withFail().withCause(e).build());
        }
        return result;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    @Override
    public CompletableFuture<AsyncResult<JmsCommonEvent>> accept(JpsEventMapperVisitor visitor) {

        CompletableFuture<AsyncResult<JmsCommonEvent>> promise = new CompletableFuture<>();
        try {
            ((JblEventMapperVisitor) visitor).map(this).thenAccept(jmsEventAsyncResult -> {

                JmsCommonEvent jmsEvent = null;

                if (jmsEventAsyncResult.result() != null) {
                    jmsEvent = jmsEventAsyncResult.result();
                }

                if (jmsEventAsyncResult.succeeded()) {
                    promise.complete(Future.succeededFuture(jmsEvent));
                } else {
                    promise.complete(Future.failedFuture(jmsEventAsyncResult.cause()));
                }
            });
        } catch (Exception e) {
            hub.jbl.common.lib.context.JBLContext.getInstance().getLogger(getClass()).error("Error on mapping " + getClass().getName() + " event!", e);
            promise.complete(Future.failedFuture(e));
        }

        return promise;
    }

    @Override
    public String eventBusPublishUrl() {
        return null;
    }

    public Long getEndRenewValidityTs() {

        return endRenewValidityTs;
    }

    public void setEndRenewValidityTs(Long endRenewValidityTs) {
        this.endRenewValidityTs = endRenewValidityTs;
    }

    public Integer getRemDailyUsg() {
        return remDailyUsg;
    }

    public void setRemDailyUsg(Integer remDailyUsg) {
        this.remDailyUsg = remDailyUsg;
    }

    public Integer getRemTotalUsg() {
        return remTotalUsg;
    }

    public void setRemTotalUsg(Integer remTotalUsg) {
        this.remTotalUsg = remTotalUsg;
    }

    public String getProdProfile() {
        return prodProfile;
    }

    public void setProdProfile(String prodProfile) {
        this.prodProfile = prodProfile;
    }

    public JpsRetCode getRetCode() {
        return retCode;
    }

    public void setRetCode(JpsRetCode retCode) {
        this.retCode = retCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getFreeTimeLeft() {
        return freeTimeLeft;
    }

    public void setFreeTimeLeft(int freeTimeLeft) {
        this.freeTimeLeft = freeTimeLeft;
    }

    public long getTimeToExit() {
        return timeToExit;
    }

    public void setTimeToExit(long timeToExit) {
        this.timeToExit = timeToExit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JpsUsrPassType getType() {
        return type;
    }

    public void setType(JpsUsrPassType type) {
        this.type = type;
    }

    public Long getEndValidityTs() {
        return endValidityTs;
    }

    public void setEndValidityTs(Long endValidityTs) {
        this.endValidityTs = endValidityTs;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Long getStartValidityTs() {
        return startValidityTs;
    }

    public void setStartValidityTs(Long startValidityTs) {
        this.startValidityTs = startValidityTs;
    }

    public String getProdProfileType() {
        return prodProfileType;
    }

    public void setProdProfileType(String prodProfileType) {
            this.prodProfileType = prodProfileType;
    }

    public BigDecimal getDeductedAmount() {
        return deductedAmount;
    }

    public void setDeductedAmount(BigDecimal deductedAmount) {
        this.deductedAmount = deductedAmount;
    }

    public long getParkingTime() {
        return parkingTime;
    }

    public void setParkingTime(long parkingTime) {
        this.parkingTime = parkingTime;
    }

    public long getPayDiffTime() {
        return payDiffTime;
    }

    public void setPayDiffTime(long payDiffTime) {
        this.payDiffTime = payDiffTime;
    }

    public Long getEntryTs() {
        return entryTs;
    }

    public void setEntryTs(Long entryTs) {
        this.entryTs = entryTs;
    }

    public Long getPayTs() {
        return payTs;
    }

    public void setPayTs(Long payTs) {
        this.payTs = payTs;
    }

    public List<Overage> getOverages() {
        if (overages == null)
            overages = new ArrayList<>();
        return overages;
    }

    /**
     * Virtual gateless entry/exits stations do not prevent
     * entry/exit even if the value in JpsUsrCardValidationResult.getRetCode() is for error.
     * In this scenario we might want to store the validation errors in warningsData list
     * and continue running the validations, even if some validation fails.
     */
    public List<JpsUsrCardValidationWarningData> getWarningsData() {
        if (warningsData == null)
            warningsData = new ArrayList<>();
        return warningsData;
    }

    public CarLocationDataDTO getCarLocationData() {
        return carLocationData;
    }

    public void setCarLocationData(CarLocationDataDTO carLocationData) {
        this.carLocationData = carLocationData;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getCardPoolName() {
        return cardPoolName;
    }

    public void setCardPoolName(String cardPoolName) {
        this.cardPoolName = cardPoolName;
    }

    public Integer getCardPoolActualValue() {
        return cardPoolActualValue;
    }

    public void setCardPoolActualValue(Integer cardPoolActualValue) {
        this.cardPoolActualValue = cardPoolActualValue;
    }

    public String getCardPoolType() {
        return cardPoolType;
    }

    public void setCardPoolType(String cardPoolType) {
        this.cardPoolType = cardPoolType;
    }

    public Long getPrepaymentExitTime() {
        return prepaymentExitTime;
    }

    public void setPrepaymentExitTime(Long prepaymentExitTime) {
        this.prepaymentExitTime = prepaymentExitTime;
    }

    public boolean getOverageForcePaymentRenewal() {
        return overageForcePaymentRenewal;
    }

    public void setOverageForcePaymentRenewal(boolean overageForcePaymentRenewal) {
        this.overageForcePaymentRenewal = overageForcePaymentRenewal;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRempinattempts() {
        return rempinattempts;
    }

    public void setRempinattempts(Integer rempinattempts) {
        this.rempinattempts = rempinattempts;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public List<JblProductProfile> getSellableProducts() {
        return sellableProducts;
    }

    public void setSellableProducts(List<JblProductProfile> sellableProducts) {
        this.sellableProducts = sellableProducts;
    }

    public Double getPricePerPoint() {
        return pricePerPoint;
    }

    public void setPricePerPoint(Double pricePerPoint) {
        this.pricePerPoint = pricePerPoint;
    }

    public String getUcValue() {
        return ucValue;
    }

    public void setUcValue(String ucValue) {
        this.ucValue = ucValue;
    }

    public String getUcClass() {
        return ucClass;
    }

    public void setUcClass(String ucClass) {
        this.ucClass = ucClass;
    }

    @Override
    public String toString() {
        return "JpsUsrCardValidationResult{" +
                "overages=" + overages +
                ", retCode=" + retCode +
                ", amount=" + amount +
                ", freeTimeLeft=" + freeTimeLeft +
                ", timeToExit=" + timeToExit +
                ", message='" + message + '\'' +
                ", endValidityTs=" + endValidityTs +
                ", endRenewValidityTs=" + endRenewValidityTs +
                ", startValidityTs=" + startValidityTs +
                ", contractNumber='" + contractNumber + '\'' +
                ", type=" + type +
                ", prodProfile='" + prodProfile + '\'' +
                ", prodProfileType='" + prodProfileType + '\'' +
                ", deductedAmount=" + deductedAmount +
                ", pricePerPoint='" + getPricePerPoint() + '\'' +
                ", parkingTime=" + parkingTime +
                ", payDiffTime=" + payDiffTime +
                ", entryTs=" + entryTs +
                ", payTs=" + payTs +
                ", licensePlate='" + licensePlate + '\'' +
                ", cardPoolName='" + cardPoolName + '\'' +
                ", cardPoolActualValue=" + cardPoolActualValue +
                ", cardPoolType='" + cardPoolType + '\'' +
                ", prepaymentExitTime=" + prepaymentExitTime +
                ", overageForcePaymentRenewal=" + overageForcePaymentRenewal +
                ", pin='" + pin + '\'' +
                ", username='" + username + '\'' +
                ", rempinattempts=" + rempinattempts +
                ", remDailyUsg=" + remDailyUsg +
                ", remTotalUsg=" + remTotalUsg +
                ", accountName='" + accountName + '\'' +
                ", reserved=" + reserved +
                ", status='" + status + '\'' +
                ", discountedAmount=" + discountedAmount +
                ", sourceCode='" + sourceCode + '\'' +
                ", uniqueSaleNumber='" + uniqueSaleNumber + '\'' +
                ", description='" + description + '\'' +
                ", ucName='" + ucName + '\'' +
                ", ucAmount='" + ucAmount + '\'' +
                ", sellableProducts='" + sellableProducts + '\'' +
                ", earlyArrivalAllowed='" + earlyArrivalAllowed + '\'' +
                ", laterDepartureAllowed='" + laterDepartureAllowed + '\'' +
                ", ucValue='" + ucValue + '\'' +
                ", ucClass='" + ucClass + '\'' +
                ", enAnonNumOfDaysUsg=" + enAnonNumOfDaysUsg +
                ", remAnonNumOfDaysUsg=" + remAnonNumOfDaysUsg +
                ", previousStayInMinutes=" + previousStayInMinutes +
                ", jmsOfflineStatus='" + jmsOfflineStatus + '\'' +
                ", operatorCardUsageType='" + operatorCardUsageType + '\'' +
                ", evtParkingDisplayMsg='" + evtParkingDisplayMsg + '\'' +
                ", evtParkingFlatFee=" + evtParkingFlatFee +
                ", evtParkingPrepaidTime=" + evtParkingPrepaidTime +
                ", evtParkingPrepayType='" + evtParkingPrepayType + '\'' +
                ", evtParkingPrepaidDays=" + evtParkingPrepaidDays +
                ", evtParkingPrepaidHHmm='" + evtParkingPrepaidHHmm + '\'' +
                ", evtParkingPrepayUC=" + evtParkingPrepayUC +
                ", gpFacilityName='" + gpFacilityName + '\'' +
                ", gpRoomNumber='" + gpRoomNumber + '\'' +
                ", gpFirstName='" + gpFirstName + '\'' +
                ", gpLastName='" + gpLastName + '\'' +
                ", additionalServices='" + additionalServices + '\'' +
                ", prodProfileEnablePostPayment='" + prodProfileEnablePostPayment + '\'' +
                ", payWithJp='" + payWithJp + '\'' +
                '}';
    }

    public List<String> getReceiptTexts() {
        return receiptTexts;
    }

    public void setReceiptTexts(List<String> receiptTexts) {
        this.receiptTexts = receiptTexts;
    }

    public Integer getReceiptCopies() {
        return receiptCopies;
    }

    public void setReceiptCopies(Integer receiptCopies) {
        this.receiptCopies = receiptCopies;
    }

    public boolean isLowBalance() {
        return lowBalance;
    }

    public void setLowBalance(boolean lowBalance) {
        this.lowBalance = lowBalance;
    }

    public Integer getFreeOfChargeTimeLeft() {
        return freeOfChargeTimeLeft;
    }

    public void setFreeOfChargeTimeLeft(Integer freeOfChargeTimeLeft) {
        this.freeOfChargeTimeLeft = freeOfChargeTimeLeft;
    }

    public boolean isProratedAmount() {
        return proratedAmount;
    }

    public void setProratedAmount(boolean proratedAmount) {
        this.proratedAmount = proratedAmount;
    }

    public String getEvtParkingDisplayMsg() {
        return evtParkingDisplayMsg;
    }

    public void setEvtParkingDisplayMsg(String evtParkingDisplayMsg) {
        this.evtParkingDisplayMsg = evtParkingDisplayMsg;
    }

    public BigDecimal getEvtParkingFlatFee() {
        return evtParkingFlatFee;
    }

    public void setEvtParkingFlatFee(BigDecimal evtParkingFlatFee) {
        this.evtParkingFlatFee = evtParkingFlatFee;
    }

    public Long getEvtParkingPrepaidTime() {
        return evtParkingPrepaidTime;
    }

    public void setEvtParkingPrepaidTime(Long evtParkingPrepaidTime) {
        this.evtParkingPrepaidTime = evtParkingPrepaidTime;
    }

    public EventParkingPrepayType getEvtParkingPrepayType() {
        return evtParkingPrepayType;
    }

    public void setEvtParkingPrepayType(EventParkingPrepayType evtParkingPrepayType) {
        this.evtParkingPrepayType = evtParkingPrepayType;
    }

    public Long getEvtParkingPrepaidDays() {
        return evtParkingPrepaidDays;
    }

    public void setEvtParkingPrepaidDays(Long evtParkingPrepaidDays) {
        this.evtParkingPrepaidDays = evtParkingPrepaidDays;
    }

    public String getEvtParkingPrepaidHHmm() {
        return evtParkingPrepaidHHmm;
    }

    public void setEvtParkingPrepaidHHmm(String evtParkingPrepaidHHmm) {
        this.evtParkingPrepaidHHmm = evtParkingPrepaidHHmm;
    }

    public Long getEvtParkingPrepayUC() {
        return evtParkingPrepayUC;
    }

    public void setEvtParkingPrepayUC(Long evtParkingPrepayUC) {
        this.evtParkingPrepayUC = evtParkingPrepayUC;
    }

    public String getOperatorCardUsageType() {
        return operatorCardUsageType;
    }

    public void setOperatorCardUsageType(String operatorCardUsageType) {
        this.operatorCardUsageType = operatorCardUsageType;
    }

    public String getCustomPointsLabel() {
        return customPointsLabel;
    }

    public void setCustomPointsLabel(String customPointsLabel) {
        this.customPointsLabel = customPointsLabel;
    }

    public Boolean getValidatedOnAps() {
        return validatedOnAps;
    }

    public void setValidatedOnAps(Boolean validatedOnAps) {
        this.validatedOnAps = validatedOnAps;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getDiscountedAmount() {
        return discountedAmount;
    }

    public void setDiscountedAmount(BigDecimal discountedAmount) {
        this.discountedAmount = discountedAmount;
    }

    public String getBayNumber() {
        return bayNumber;
    }

    public void setBayNumber(String bayNumber) {
        this.bayNumber = bayNumber;
    }

    public String getTagTelepass() {
        return tagTelepass;
    }

    public void setTagTelepass(String tagTelepass) {
        this.tagTelepass = tagTelepass;
    }

    public String getUniqueSaleNumber() {
        return uniqueSaleNumber;
    }

    public void setUniqueSaleNumber(String uniqueSaleNumber) {
        this.uniqueSaleNumber = uniqueSaleNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUcName() {
        return ucName;
    }

    public void setUcName(String ucName) {
        this.ucName = ucName;
    }

    public BigDecimal getUcAmount() {
        return ucAmount;
    }

    public void setUcAmount(BigDecimal ucAmount) {
        this.ucAmount = ucAmount;
    }

    public String getOldLstUid() {
        return oldLstUid;
    }

    public void setOldLstUid(String oldLstUid) {
        this.oldLstUid = oldLstUid;
    }

    public CalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(CalcType calcType) {
        this.calcType = calcType;
    }

    public int getEarlyArrivalAllowed() {
        return earlyArrivalAllowed;
    }

    public void setEarlyArrivalAllowed(int earlyArrivalAllowed) {
        this.earlyArrivalAllowed = earlyArrivalAllowed;
    }

    public int getLaterDepartureAllowed() {
        return laterDepartureAllowed;
    }

    public void setLaterDepartureAllowed(int laterDepartureAllowed) {
        this.laterDepartureAllowed = laterDepartureAllowed;
    }

    public boolean isEnAnonNumOfDaysUsg() {
        return enAnonNumOfDaysUsg;
    }

    public void setEnAnonNumOfDaysUsg(boolean enAnonNumOfDaysUsg) {
        this.enAnonNumOfDaysUsg = enAnonNumOfDaysUsg;
    }

    public Integer getRemAnonNumOfDaysUsg() {
        return remAnonNumOfDaysUsg;
    }

    public void setRemAnonNumOfDaysUsg(Integer remAnonNumOfDaysUsg) {
        this.remAnonNumOfDaysUsg = remAnonNumOfDaysUsg;
    }

    public long getPreviousStayInMinutes() {
        return previousStayInMinutes;
    }

    public void setPreviousStayInMinutes(long previousStayInMinutes) {
        this.previousStayInMinutes = previousStayInMinutes;
    }

    public Boolean getJmsOfflineStatus() {
        return jmsOfflineStatus;
    }

    public void setJmsOfflineStatus(Boolean jmsOfflineStatus) {
        this.jmsOfflineStatus = jmsOfflineStatus;
    }

    public JblTransientUsrPass getTransientUsrPass() {
        return transientUsrPass;
    }

    public void setTransientUsrPass(JblTransientUsrPass transientUsrPass) {
        this.transientUsrPass = transientUsrPass;
    }

    public boolean isMembershipExpired() {
        return isMembershipExpired;
    }

    public void setMembershipExpired(boolean membershipExpired) {
        isMembershipExpired = membershipExpired;
    }

    public String getMembershipUuid() {
        return membershipUuid;
    }

    public void setMembershipUuid(String membershipUuid) {
        this.membershipUuid = membershipUuid;
    }

    public Map<Double, Short> getPointsAmountForQty() {
        return pointsAmountForQty;
    }

    public void setPointsAmountForQty(Map<Double, Short> pointsAmountForQty) {
        this.pointsAmountForQty = pointsAmountForQty;
    }

    public String getGpFacilityName() {
        return gpFacilityName;
    }

    public void setGpFacilityName(String gpFacilityName) {
        this.gpFacilityName = gpFacilityName;
    }

    public String getGpRoomNumber() {
        return gpRoomNumber;
    }

    public void setGpRoomNumber(String gpRoomNumber) {
        this.gpRoomNumber = gpRoomNumber;
    }

    public String getGpFirstName() {
        return gpFirstName;
    }

    public void setGpFirstName(String gpFirstName) {
        this.gpFirstName = gpFirstName;
    }

    public String getGpLastName() {
        return gpLastName;
    }

    public void setGpLastName(String gpLastName) {
        this.gpLastName = gpLastName;
    }

    public BigDecimal getTaxAmountBWI() {
        return taxAmountBWI;
    }

    public void setTaxAmountBWI(BigDecimal taxAmountBWI) {
        this.taxAmountBWI = taxAmountBWI;
    }

    public Integer getTaxableNumberOfDaysBWI() {
        return taxableNumberOfDaysBWI;
    }

    public void setTaxableNumberOfDaysBWI(Integer taxableNumberOfDaysBWI) {
        this.taxableNumberOfDaysBWI = taxableNumberOfDaysBWI;
    }

    public JpsAdditionalServiceDataDTO getAdditionalServices() {
        return additionalServices;
    }

    public void setAdditionalServices(JpsAdditionalServiceDataDTO additionalServices) {
        this.additionalServices = additionalServices;
    }

    public Boolean getProdProfileEnablePostPayment() {
        return prodProfileEnablePostPayment;
    }

    public void setProdProfileEnablePostPayment(Boolean prodProfileEnablePostPayment) {
        this.prodProfileEnablePostPayment = prodProfileEnablePostPayment;
    }

    public BigDecimal getPosDiscountedAmount() {
        return posDiscountedAmount;
    }

    public void setPosDiscountedAmount(BigDecimal posDiscountedAmount) {
        this.posDiscountedAmount = posDiscountedAmount;
    }

    public String getPosDiscDescr() {
        return posDiscDescr;
    }

    public void setPosDiscDescr(String posDiscDescr) {
        this.posDiscDescr = posDiscDescr;
    }

    public BigDecimal getCashDiscountedAmount() {
        return cashDiscountedAmount;
    }

    public void setCashDiscountedAmount(BigDecimal cashDiscountedAmount) {
        this.cashDiscountedAmount = cashDiscountedAmount;
    }

    public String getCashDiscDescr() {
        return cashDiscDescr;
    }

    public void setCashDiscDescr(String cashDiscDescr) {
        this.cashDiscDescr = cashDiscDescr;
    }

    public BigDecimal getProvsDiscountedAmount() {
        return provsDiscountedAmount;
    }

    public void setProvsDiscountedAmount(BigDecimal provsDiscountedAmount) {
        this.provsDiscountedAmount = provsDiscountedAmount;
    }

    public String getProvsDiscDescr() {
        return provsDiscDescr;
    }

    public void setProvsDiscDescr(String provsDiscDescr) {
        this.provsDiscDescr = provsDiscDescr;
    }

    public Boolean getPayWithJp() {
        return payWithJp;
    }

    public void setPayWithJp(Boolean payWithJp) {
        this.payWithJp = payWithJp;
    }

    public static class Builder {
        private JpsUsrCardValidationResult instance;

        public Builder() {
            instance = new JpsUsrCardValidationResult();
        }

        public Builder withRetCode(JpsRetCode retCode) {
            instance.setRetCode(retCode);
            return this;
        }

        public Builder withAmount(BigDecimal amount) {
            instance.setAmount(amount);
            return this;
        }

        public Builder withFreeTimeLeft(int freeTimeLeft) {
            instance.setFreeTimeLeft(freeTimeLeft);
            return this;
        }

        public Builder withTimeToExit(long timeToExit) {
            instance.setTimeToExit(timeToExit);
            return this;
        }

        public Builder withMessage(String message) {
            instance.setMessage(message);
            return this;
        }


        public Builder withUsrPass(JpsLogUsrPass usrPass) {
            instance.setUsrPass(usrPass);
            return this;
        }

        public Builder with(JpsUsrCardValidationResult result) {
            instance = result;
            return this;
        }

        public JpsUsrCardValidationResult build() {
            return instance;
        }

        public JpsUsrCardValidationResult getInstance() {
            return instance;
        }

        public void setInstance(JpsUsrCardValidationResult instance) {
            this.instance = instance;
        }
    }
}
