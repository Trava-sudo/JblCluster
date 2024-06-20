package hub.ebb.jblcluster.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Date;

public class RestVerticle extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        final Router router = Router.router(vertx);
        router.get("/test-app/verification/get").handler(this::get);
        router.post("/test-app/verification/post").handler(this::post);
        router.put("/test-app/verification/put").handler(this::put);
        router.delete("/test-app/verification/delete").handler(this::delete);
        HttpServerOptions httpServerOptions = new HttpServerOptions().setIdleTimeout(60).setTcpKeepAlive(true).setReuseAddress(true);
        vertx.createHttpServer(httpServerOptions).requestHandler(router).listen(64327, result -> {
            if (result.succeeded()) {
                logger.info("Rest Endpoint is running on port " + 64327);
                startPromise.complete();
            } else {
                logger.error("Failed listening on port: " + 64327, result.cause());
                startPromise.fail(result.cause());
            }
        });
    }

    private void delete(RoutingContext routingContext) {
        reply("delete", routingContext);
    }

    private void put(RoutingContext routingContext) {
        reply("put", routingContext);
    }

    private void post(RoutingContext routingContext) {
        reply("post", routingContext);
    }

    private void get(RoutingContext routingContext) {
        reply("get", routingContext);
    }

    private void reply(String delete, RoutingContext routingContext) {

        JsonObject reply = new JsonObject();
        reply.put("method", delete);
        reply.put("time", new Date().toString());
        reply.put("path", routingContext.request().path());
        JsonObject bodyAsJson = routingContext.getBodyAsJson();
        if (bodyAsJson != null)
            reply.put("request", bodyAsJson);
        routingContext.response().setStatusCode(200).end(reply.encodePrettily());
    }
}
