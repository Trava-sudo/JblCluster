package hub.ebb.jblcluster.eventservice.mapper;


import hub.jbl.core.visitor.jps.JpsVisitableEvent;
import io.vertx.core.AsyncResult;

import java.util.concurrent.CompletableFuture;

public interface JblVisitableEvent extends JpsVisitableEvent{

    CompletableFuture<AsyncResult<JmsEvent>> accept(JblEventMapperVisitor visitor);
}
