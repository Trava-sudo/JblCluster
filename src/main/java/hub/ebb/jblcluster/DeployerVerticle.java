package hub.ebb.jblcluster;


import hub.ebb.jblcluster.verticles.PingerVerticle;
import hub.ebb.jblcluster.verticles.RestVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DeployerVerticle extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        deployVerticle(new PingerVerticle()).
                compose(f -> deployVerticle(new RestVerticle())).
                onFailure(error -> startPromise.fail(error)).
                onSuccess(success -> startPromise.complete());
    }

    private Future<String> deployVerticle(AbstractVerticle verticle) {
        Promise<String> promise = Promise.promise();
        final DeploymentOptions options = new DeploymentOptions().setConfig(config());
        vertx.deployVerticle(verticle, options, b -> {
            if (b.succeeded()) {
                logger.info("Correctly deployed verticle " + verticle.getClass().getSimpleName());
                promise.handle(Future.succeededFuture());
            } else {
                logger.error("Cannot deploy verticle " + verticle.getClass().getSimpleName(), b.cause());
                promise.handle(Future.failedFuture(b.cause()));
            }
        });

        return promise.future();
    }
}
