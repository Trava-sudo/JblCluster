package hub.ebb.jblcluster.verticles.jpsEvent;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.serviceproxy.ServiceBinder;

public abstract class AbstractPlusRegisterProxyVerticle extends AbstractVerticle {

    public <T> MessageConsumer bindServiceToProxy(String proxyAddress, Class<T> proxyAndServiceClass, T service){
        return new ServiceBinder(vertx).setAddress(proxyAddress).register(proxyAndServiceClass, service);
    }

}
