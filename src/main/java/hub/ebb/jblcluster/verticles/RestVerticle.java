package hub.ebb.jblcluster.verticles;

import hub.ebb.jblcluster.eventservice.web.JpsEventsWepApi;
import hub.ebb.jblcluster.verticles.jpsEvent.AbstractPlusRegisterProxyVerticle;
import hub.ebb.jblcluster.verticles.jpsEvent.JpsEventVerticle;
import hub.ebb.jblcluster.eventservice.web.ValidationFactoryWebApi;
import hub.jbl.common.lib.context.JBLContext;
import hub.jbl.common.lib.log.Logger;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
import io.vertx.ext.web.openapi.RouterBuilder;

public class RestVerticle extends AbstractPlusRegisterProxyVerticle {

    Logger logger = JBLContext.getInstance().getLogger(getClass());
    private Integer listeningPort;


    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        bindServiceToProxy("api.event", JpsEventsWepApi.class, new JpsEventVerticle());
        createRouterFromDocumentation(vertx, startPromise);
    }

    private void createRouterFromDocumentation(Vertx vertx, Promise<Void> startPromise) {
        // Evaluate implementation of PostmanAPI for collection retrieval
        RouterBuilder.create(vertx, "./RestAPI.json")
                .onSuccess(builder -> {
                    setListeningPort(56789);
                    defineOperations(builder);
                    final Router router = builder.createRouter();
                    HttpServerOptions httpServerOptions = new HttpServerOptions().setIdleTimeout(60).setTcpKeepAlive(true).setReuseAddress(true);
                    vertx.createHttpServer(httpServerOptions).requestHandler(router).listen(getListeningPort(), result -> {
                        if (result.succeeded()) {
                            logger.info(String.format("Rest Endpoint is running on port %s", getListeningPort()));
                            startPromise.complete();
                        } else {
                            logger.error(String.format("Failed listening on port: %s", getListeningPort()), result.cause());
                            startPromise.fail(result.cause());
                        }
                    });
                })
                .onFailure(error -> {
                    logger.error(error);
                    startPromise.fail(error);
                });
    }



    private void defineOperations(RouterBuilder builder) {
        var validationFactory = new ValidationFactoryWebApi();
        builder.operation("doEventManagement")
                .handler(validationFactory.getValidationHandler(vertx, "doEventManagement"))
                .handler(RouteToEBServiceHandler.build(vertx.eventBus(), validationFactory.getAddress(), "doEventManagement"));

    }

    public Integer getListeningPort() {
        return listeningPort;
    }

    public void setListeningPort(Integer listeningPort) {
        this.listeningPort = listeningPort;
    }
}

