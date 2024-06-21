package hub.ebb.jblcluster.eventservice.web;

import hub.jbl.common.lib.webapi.AbstractBaseValidatorFactory;
import io.vertx.core.Vertx;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;

import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public class ValidationFactoryWebApi extends AbstractBaseValidatorFactory {

    static final String REST_CUSTOMER_BASE_PATH = "/jbl/api/customer/";
    static final String EB_CUSTOMER_ADDR = REST_CUSTOMER_BASE_PATH.replace("/", ".");

    public ValidationHandler getValidationHandler(Vertx vertx, String actionName) {
        switch (actionName) {
            case "doEventManagement":

                return ValidationHandlerBuilder.create(getSchemaParser(vertx)).
                        pathParameter(Parameters.param("sequenceNumberTS", numberSchema())).
                        pathParameter(Parameters.param("sequenceNumberGMT", numberSchema())).
                        pathParameter(Parameters.param("sequenceNumberCounter", numberSchema())).
                        pathParameter(Parameters.param("peripheralId", stringSchema())).
                        body(getBodyProcessorFactory()).build();
            default:
                throw new IllegalArgumentException("ActionName " + actionName + " does not exists!");
        }
    }

    public String getAddress() {
        return EB_CUSTOMER_ADDR;
    }
}
