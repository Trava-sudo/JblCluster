package hub.ebb.jblcluster.verticles.jpsEvent;

import hub.jbl.common.lib.SessionFields;
import hub.jbl.common.lib.api.peripheral.JpsAuthenticatedPeripheralAPI;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.utils.HttpServerUtils;
import hub.jbl.common.verticles.AbstractRestVerticle;
import hub.jbl.core.dto.jps.event.JpsUsrPassMediaType;
import hub.jbl.common.dao.authentication.JpsAuthenticatedPeripheral;
import hub.ebb.jblcluster.eventservice.model.SellableProductType;
import hub.ebb.jblcluster.eventservice.service.JpsContractIssuingService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class JpsContractIssuingVerticle extends AbstractRestVerticle {

    public static String JPS_PRODUCT_MICROSERVICE_PATH = "\\/jbl\\/api\\/products\\/.*";

    public static String JPS_PRODUCT_ENDPOINT_PATH = "/jbl/api/products";

    @Autowired
    private JpsContractIssuingService service;
    @Autowired
    private JpsAuthenticatedPeripheralAPI jpsAuthenticatedPeripheralAPI;

    @Override
    protected void handleRestEndpoint(Router router) {
        router.get(getServiceEndpoint() + "/:productType").handler(this::retrieveProductType);
    }

    private void retrieveProductType(RoutingContext routingContext) {
        JBLContext context = context(routingContext);
        jpsAuthenticatedPeripheralAPI.doCheckAuthenticationJPS(HttpServerUtils.token(routingContext), peripheralAsyncResult -> {
            if (peripheralAsyncResult.succeeded()) {
                final JpsAuthenticatedPeripheral authenticatedPeripheral = JpsAuthenticatedPeripheral.fromJson(peripheralAsyncResult.result());
                String type = authenticatedPeripheral.getType();
                SellableProductType productType = SellableProductType.fromType(type);

                if (productType != null) {
                    JpsUsrPassMediaType mediaType = JpsUsrPassMediaType.LPR;
                    if (routingContext.request().getHeader("jbl_mediatype") != null)
                        mediaType = JpsUsrPassMediaType.valueOf(routingContext.request().getHeader("jbl_mediatype"));

                    String identifier;
                    if (mediaType.equals(JpsUsrPassMediaType.QRCodePass)) {
                        identifier = routingContext.request().getHeader("jbl_rawdata");
                    } else {
                        identifier = routingContext.request().getHeader("oldLstUid");
                    }
                    String membershipUuid = routingContext.request().getHeader("membershipUuid");

                    if (StringUtils.isNotBlank(membershipUuid)) {
                        context.getLogger(this.getClass()).info("RECEIVED new request from JPS for getSellableProductProfilesForMembership");
                        service.getSellableProductsForMembership(context, membershipUuid, identifier, mediaType , getSellableProductsForMembershipAsyncResult -> {
                            if (getSellableProductsForMembershipAsyncResult.succeeded()) {
                                context.addSessionField(SessionFields.MEMBERSHIP_CONTRACT_REPLACE, true);
                                HttpServerUtils.responseOk(getSellableProductsForMembershipAsyncResult.result().encodePrettily(), routingContext);
                            } else {
                                JsonObject error = new JsonObject();
                                error.put("error", "Unable to get sellable products for membership");
                                HttpServerUtils.responseKO(error.encodePrettily(), routingContext);
                            }
                        });
                    } else {
                        service.getSellableProducts(context, productType, identifier, asyncResult -> {
                            if (asyncResult.succeeded()) {
                                HttpServerUtils.responseOk(asyncResult.result().encodePrettily(), routingContext);
                            } else {
                                JsonObject error = new JsonObject();
                                error.put("error", "Error");
                                HttpServerUtils.responseKO(error.encodePrettily(), routingContext);
                            }
                        });
                    }
                } else {
                    JsonObject error = new JsonObject();
                    error.put("error", "Product Type Not found");
                    HttpServerUtils.responseKO(error.encodePrettily(), routingContext);
                }
            } else {
                HttpServerUtils.responseNotAuthorized(routingContext);
            }
        });
    }


    @Override
    protected String getServiceEndpoint() {
        return JPS_PRODUCT_ENDPOINT_PATH;
    }

    @Override
    protected String getRegExpMicroServiceName() {
        return JPS_PRODUCT_MICROSERVICE_PATH;
    }
}
