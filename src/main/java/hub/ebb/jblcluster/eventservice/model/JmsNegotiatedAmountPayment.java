package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.core.dto.jps.event.JpsOpUsrPayCompl;
import hub.jbl.core.visitor.jps.JmsCommonEvent;
import hub.jbl.core.visitor.jps.JpsEventMapperVisitor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import java.util.concurrent.CompletableFuture;

public class JmsNegotiatedAmountPayment extends JpsOpUsrPayCompl {

    @Override
    public CompletableFuture<AsyncResult<JmsCommonEvent>> accept(JpsEventMapperVisitor visitor) {
        CompletableFuture<AsyncResult<JmsCommonEvent>> result = new CompletableFuture<>();
        try {
            result = visitor.map(this);
        } catch (Exception e) {
            hub.jbl.common.lib.context.JBLContext.getInstance().getLogger(getClass()).error("Error on mapping " + getClass().getName() + " event.", e);
            result.complete(Future.failedFuture(e));
        }
        return result;
    }

}
