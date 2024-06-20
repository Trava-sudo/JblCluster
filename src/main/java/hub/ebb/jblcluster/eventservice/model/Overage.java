package hub.ebb.jblcluster.eventservice.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Overage implements Serializable {

    private long startOverage, endOverage;
    private BigDecimal amount;
    private String sourceCode;

    public Overage() {
    }

    public Overage(long startOverage, long endOverage, BigDecimal amount) {
        this.startOverage = startOverage;
        this.endOverage = endOverage;
        this.amount = amount;
    }

    public long getStartOverage() {
        return startOverage;
    }

    public long getEndOverage() {
        return endOverage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setStartOverage(long startOverage) {
        this.startOverage = startOverage;
    }

    public void setEndOverage(long endOverage) {
        this.endOverage = endOverage;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public long getLengthInMilli() {

        return (this.getEndOverage() - this.getStartOverage());
    }

}
