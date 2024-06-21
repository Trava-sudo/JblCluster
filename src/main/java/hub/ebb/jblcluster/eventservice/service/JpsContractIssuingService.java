package hub.ebb.jblcluster.eventservice.service;

import hub.ebb.jblcluster.eventservice.model.SellableProductType;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.utils.handler.JblHandler;
import hub.jbl.core.dto.jps.event.JpsUsrPassMediaType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;


public interface JpsContractIssuingService {
    void getSellableProducts(JBLContext context, SellableProductType productType, String identifier, Handler<AsyncResult<JsonArray>> asyncResultHandler);

    void getSellableProductsForMembership(JBLContext context, String membershipUuid, String identifier, JpsUsrPassMediaType mediaType, JblHandler<JsonArray> resultHandler);
}
