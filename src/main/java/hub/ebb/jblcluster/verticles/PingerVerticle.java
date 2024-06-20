package hub.ebb.jblcluster.verticles;

import hub.ebb.ext.APIs.inbound.configuration.ConfigurationAPI;
import hub.ebb.ext.APIs.outbound.pinger.PingerAPI;
import hub.ebb.ext.APIs.outbound.pinger.dto.PingerResponseDto;
import hub.jbl.common.lib.date.DateUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;
import org.joda.time.DateTime;

public class PingerVerticle extends AbstractVerticle implements PingerAPI {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ConfigurationAPI configurationAPI;

  @Override
  public void start() {
    new ServiceBinder(vertx).setAddress(config().getString("moduleName")).register(PingerAPI.class, this);
    configurationAPI = ConfigurationAPI.createProxy(vertx, ConfigurationAPI.ADDRESS);
  }

  @Override
  public void ping(boolean reloadConfig, Handler<AsyncResult<PingerResponseDto>> handler) {
    final DateTime now = DateTime.now();
    logger.info("Receive ping from Jbl at " + DateUtils.getTime(now));
    final PingerResponseDto pingerResponseDto = new PingerResponseDto(config().getString("moduleName"), now.getMillis(), DateUtils.getGMTInMinutes(now.getMillis()));
    handler.handle(Future.succeededFuture(pingerResponseDto));


  }

}
