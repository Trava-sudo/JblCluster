package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.common.lib.builder.AsyncResultBuilder;
import hub.jbl.core.dto.jps.event.JpsEvtAlarm;
import hub.jbl.core.visitor.jps.JmsCommonEvent;
import hub.jbl.core.visitor.jps.JpsEventMapperVisitor;
import hub.jbl.eventservice.mapper.JblEventMapperVisitor;
import hub.jbl.eventservice.mapper.JblVisitableEvent;
import hub.jbl.eventservice.mapper.JmsEvent;
import hub.jbl.eventservice.service.jmsMapper.JmsMessageIDBundle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.concurrent.CompletableFuture;

public class JblAlarmUpdateTariffCommand extends JpsEvtAlarm implements JblVisitableEvent {

    @Override
    public Long messageId() {
        return JmsMessageIDBundle.ALARM_JBL_UDPATE_TARIFF_COMMAND;
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

}
