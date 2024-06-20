package hub.ebb.jblcluster.eventservice.model;

import java.math.BigDecimal;
import java.util.List;

public class ApplyDiscountsResult {

    private ApplyDiscountValidationResult validationResult;

    private BigDecimal deductedAmount;
    private BigDecimal finalAmount;
    private List<ApplyDiscountResult> appliedDiscountResults;

    public BigDecimal getDeductedAmount() {
        return deductedAmount;
    }
    public void setDeductedAmount(BigDecimal deductedAmount) {
        this.deductedAmount = deductedAmount;
    }

    public List<ApplyDiscountResult> getAppliedDiscountResults() {
        return appliedDiscountResults;
    }

    public void setAppliedDiscountResults(List<ApplyDiscountResult> appliedDiscountResults) {
        this.appliedDiscountResults = appliedDiscountResults;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public ApplyDiscountValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ApplyDiscountValidationResult validationResult) {
        this.validationResult = validationResult;
    }
}
