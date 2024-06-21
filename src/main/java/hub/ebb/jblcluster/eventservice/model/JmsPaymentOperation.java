package hub.ebb.jblcluster.eventservice.model;

import hub.ebb.jblcluster.eventservice.mapper.JblEventMapperVisitor;
import hub.ebb.jblcluster.eventservice.mapper.JblVisitableEvent;
import hub.ebb.jblcluster.eventservice.mapper.JmsEvent;
import hub.jbl.common.lib.builder.AsyncResultBuilder;
import hub.jbl.core.dto.jps.event.JpsOpUsrPayCompl;
import hub.jbl.core.visitor.jps.JmsCommonEvent;
import hub.jbl.core.visitor.jps.JpsEventMapperVisitor;
import hub.jms.common.model.account.invoice.PostPaymentMethodType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class JmsPaymentOperation extends JpsOpUsrPayCompl implements JblVisitableEvent {

    private long invoiceId;
    private PostPaymentMethodType type;
    private BigDecimal amount;
    private String notes;
    private String payer;
    private String cardIdentifier;
    private String contractIdentifier;
    private String entityId;
    private long paymentId;
    private long periodFrom;
    private Short periodFromGmt;
    private long periodTo;
    private Short periodToGmt;

    @Override
    public String eventBusPublishUrl() {
        return null;
    }

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
            hub.jbl.common.lib.context.JBLContext.getInstance().getLogger(getClass()).error("Error on mapping " + getClass().getName() + " event.", e);
            promise.complete(Future.failedFuture(e));
        }

        return promise;
    }

    public long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public PostPaymentMethodType getType() {
        return type;
    }

    public void setType(PostPaymentMethodType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public String getCardIdentifier() {
        return cardIdentifier;
    }

    public void setCardIdentifier(String cardIdentifier) {
        this.cardIdentifier = cardIdentifier;
    }

    public String getContractIdentifier() {
        return contractIdentifier;
    }

    public void setContractIdentifier(String contractIdentifier) {
        this.contractIdentifier = contractIdentifier;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
    }

    public long getPeriodFrom() {
        return periodFrom;
    }

    public void setPeriodFrom(long periodFrom) {
        this.periodFrom = periodFrom;
    }

    public Short getPeriodFromGmt() {
        return periodFromGmt;
    }

    public void setPeriodFromGmt(Short periodFromGmt) {
        this.periodFromGmt = periodFromGmt;
    }

    public long getPeriodTo() {
        return periodTo;
    }

    public void setPeriodTo(long periodTo) {
        this.periodTo = periodTo;
    }

    public Short getPeriodToGmt() {
        return periodToGmt;
    }

    public void setPeriodToGmt(Short periodToGmt) {
        this.periodToGmt = periodToGmt;
    }
}
