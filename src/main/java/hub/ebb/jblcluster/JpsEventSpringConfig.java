package hub.ebb.jblcluster;

import hub.ebb.jblcluster.eventservice.service.JblCounterSourceService;
import hub.ebb.jblcluster.eventservice.service.JpsContractIssuingService;
import hub.ebb.jblcluster.eventservice.service.JpsEventService;
import hub.ebb.jblcluster.eventservice.service.impl.JpsContractIssuingServiceImpl;
import hub.jbl.common.applybypaymenttype.CalculationOfDiscountOnPaymentType;
import hub.jbl.common.lib.R;
import hub.jbl.common.lib.api.customer.CustomerAPI;
import hub.jbl.common.lib.api.event.EventAPI;
import hub.jbl.common.lib.api.peripheral.JpsAuthenticatedPeripheralAPI;
import hub.jbl.common.lib.api.plate.RemotePlateSearchAPI;
import hub.jbl.common.lib.api.productprofile.ProductAPI;
import hub.jbl.common.lib.api.validation.DiscountOnPaymentTypeAPI;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.common.services.JblTransactionManagerImpl;
import hub.jbl.common.session.RemoteDiscountSessionData;
import hub.jbl.common.session.RemoteDiscountSessionDataImpl;
import hub.jbl.common.spring.AppSpringConfig;
import hub.ebb.jblcluster.verticles.jpsEvent.JpsEventVerticle;
import hub.jbl.dao.*;
import hub.jbl.dao.impl.*;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

/**
 * Created by Stefano.Coletta on 18/10/2016.
 */
public class JpsEventSpringConfig extends AppSpringConfig {


    @Bean
    public JpsEventService jpsEventService() {
        return new JpsEventService();
    }

    @Bean
    public JblCounterSourceService jblCounterService(Vertx vertx) {
        return new JblCounterSourceService(vertx);
    }

    @Bean
    public JpsContractIssuingService jpsContractIssuingServiceImpl() {
        return new JpsContractIssuingServiceImpl();
    }

    @Bean
    public JpsEventVerticle jpsEventVerticle() {
        return new JpsEventVerticle();
    }

    @Bean
    @Qualifier("proxy")
    public RemotePlateSearchAPI remotePlateSearchAPIProxy(Vertx vertx) {
        return RemotePlateSearchAPI.createProxy(vertx);
    }

    @Bean
    public EventAPI eventAPI(Vertx vertx) {
        return EventAPI.createProxy(vertx);
    }

    @Bean
    public JblEventDao jblEventDao() {
        return new JblEventDaoImpl();
    }

    @Bean
    public JblAlarmDao jblAlarmDao() {
        return new JblAlarmDaoImpl();
    }

    @Bean
    public JblTransactionManager transactionManager(@Qualifier("connectionPool") JDBCClient jdbcClient) {
        return new JblTransactionManagerImpl(jdbcClient, "JblDefaultTransactionManager");
    }

    @Bean
    public FiscalPrinterDao fiscalPrinterDao() {
        return new FiscalPrinterDaoImpl();
    }

    @Bean
    public FiscalNumberCounterDao fiscalNumberCounterDao() {
        return new FiscalNumberCounterDaoImpl();
    }

    @Bean
    public JblEventToContractDao jblEventToContractDao() {
        return new JblEventToContractDaoImpl();
    }

    @Bean
    public FcjOptorShiftDao fcjOptorShiftDao() {
        return new FcjOptorShiftDaoImpl();
    }

    @Bean
    public JblConfigDao jblConfigDao() {
        return new JblConfigDaoImpl();
    }

    @Bean
    public JblParkNodeDao jblParkNodeDao() {
        return new JblParkNodeDaoImpl();
    }

    @Bean
    public JblParkCounterSourceDao jblParkCounterSourceDao() {
        return new JblParkCounterSourceDaoImpl();
    }

    @Bean
    public JblParkCounterSourceFilterSetDao jblParkCounterSourceFilterSetDao() {
        return new JblParkCounterSourceFilterSetDaoImpl();
    }

    @Bean
    public JblParkCounterSourceFilterDao jblParkCounterSourceFilterDao() {
        return new JblParkCounterSourceFilterDaoImpl();
    }

    @Bean
    public ProductProfileDao productProfileDao() {
        return new ProductProfileDaoImpl();
    }

    @Bean
    public TransientUsrPassDao jblTransientUsrPassDao() {
        return new TransientUsrPassDaoImpl();
    }

    @Bean
    public MembershipDao membershipDao() {
        return new MembershipDaoImpl();
    }

    @Bean
    @Qualifier("productAPI")
    public ProductAPI productAPI(Vertx vertx) {
        return ProductAPI.createProxy(vertx, R.PROXY_API_PRODUCT);
    }

    @Bean
    @Qualifier("customerAPI")
    public CustomerAPI customerAPI(Vertx vertx) {
        return CustomerAPI.createProxy(vertx, R.PROXY_API_CUSTOMER);
    }

    @Bean
    public DiscountOnPaymentTypeAPI discountOnPaymentTypeAPI(Vertx vertx) {
        return DiscountOnPaymentTypeAPI.createProxy(vertx);
    }

    @Bean
    public MembershipMediaTypesDao membershipMediaTypesDao() {
        return new MembershipMediaTypesDaoImpl();
    }

    @Bean("jpsAuthenticatedPeripheralAPI")
    public JpsAuthenticatedPeripheralAPI jpsAuthenticatedPeripheralAPI(Vertx vertx) {
        return JpsAuthenticatedPeripheralAPI.createProxy(vertx, R.PROXY_API_PERIPHERALS);
    }

    @Bean
    public RemoteDiscountSessionData remoteDiscountSessionData() {
        return new RemoteDiscountSessionDataImpl();
    }
}
