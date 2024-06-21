package hub.ebb.jblcluster.eventservice.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import io.vertx.ext.web.api.service.WebApiServiceGen;

@WebApiServiceGen
public interface JpsEventsWepApi {

    void doEventManagement(long seqTS, int seqGMT, int seqCounter, String peripheralId, ServiceRequest serviceRequest, Handler<AsyncResult<ServiceResponse>> handler);
}
