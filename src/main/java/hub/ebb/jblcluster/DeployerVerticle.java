package hub.ebb.jblcluster;


import hub.ebb.jblcluster.verticles.PingerVerticle;
import hub.ebb.jblcluster.verticles.RestVerticle;
import hub.ebb.jblcluster.verticles.jpsEvent.JpsEventVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class DeployerVerticle extends AbstractVerticle {

    private static final String CONFIG_BEAN_NAME = "config";
    private static final String VERTX_BEAN_NAME = "vertx";

    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        vertx.<ApplicationContext>executeBlocking(blockingPromise -> {
            AnnotationConfigApplicationContext applicationContext = null;
            try {
                applicationContext = new AnnotationConfigApplicationContext();
                applicationContext.register(JpsEventSpringConfig.class);
                applicationContext.getBeanFactory().registerSingleton(VERTX_BEAN_NAME, vertx);
                applicationContext.getBeanFactory().registerSingleton(CONFIG_BEAN_NAME, config());
                applicationContext.refresh();
                blockingPromise.handle(Future.succeededFuture(applicationContext));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                blockingPromise.handle(Future.failedFuture(e));
            }

        }, event -> {
            if (event.succeeded()) {
                ApplicationContext applicationContext = event.result();
                deployVerticle(applicationContext.getBean(PingerVerticle.class)).
                        compose(f -> deployVerticle(applicationContext.getBean(JpsEventVerticle.class))).
                        compose(f -> deployVerticle(applicationContext.getBean(RestVerticle.class))).
                        onFailure(error -> startPromise.fail(error)).
                        onSuccess(success -> startPromise.complete());
            } else {
                startPromise.fail(event.cause());
            }
        });

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
