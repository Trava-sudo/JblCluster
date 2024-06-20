package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.entity.cardvalidation.ExternalValidationSourceType;

import java.math.BigDecimal;

public class ApplyDiscountResult {

    private Long discountId;
    private Long shopId;
    private BigDecimal discountAmount;
    private BigDecimal originalAmount;
    private String discountDescription;
    private Long deductionTime;
    private Double percentageCharge;
    private ExternalValidationSourceType externalValidationSourceType;
    private ApplyDiscountValidationResult validationResult;

    public ApplyDiscountValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ApplyDiscountValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountDescription() {
        return discountDescription;
    }

    public void setDiscountDescription(String discountDescription) {
        this.discountDescription = discountDescription;
    }

    public Long getDeductionTime() {
        return deductionTime;
    }

    public void setDeductionTime(Long deductionTime) {
        this.deductionTime = deductionTime;
    }

    public Double getPercentageCharge() {
        return percentageCharge;
    }

    public void setPercentageCharge(Double percentageCharge) {
        this.percentageCharge = percentageCharge;
    }

    public Long getDiscountId() {
        return discountId;
    }

    public void setDiscountId(Long discountId) {
        this.discountId = discountId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public ExternalValidationSourceType getExternalValidationSourceType() {
        return externalValidationSourceType;
    }

    public void setExternalValidationSourceType(ExternalValidationSourceType externalValidationSourceType) {
        this.externalValidationSourceType = externalValidationSourceType;
    }
}
