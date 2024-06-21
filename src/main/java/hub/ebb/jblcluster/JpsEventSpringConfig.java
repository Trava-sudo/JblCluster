package hub.ebb.jblcluster;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hub.ebb.jblcluster.eventservice.service.JblCounterSourceService;
import hub.ebb.jblcluster.eventservice.service.JpsContractIssuingService;
import hub.ebb.jblcluster.eventservice.service.JpsEventService;
import hub.ebb.jblcluster.eventservice.service.impl.JpsContractIssuingServiceImpl;
import hub.ebb.jblcluster.verticles.RestVerticle;
import hub.ebb.jblcluster.verticles.jpsEvent.JpsEventVerticle;
import hub.jbl.common.crypto.CryptoPassword;
import hub.jbl.common.lib.R;
import hub.jbl.common.lib.api.customer.CustomerAPI;
import hub.jbl.common.lib.api.event.EventAPI;
import hub.jbl.common.lib.api.peripheral.JpsAuthenticatedPeripheralAPI;
import hub.jbl.common.lib.api.plate.RemotePlateSearchAPI;
import hub.jbl.common.lib.api.productprofile.ProductAPI;
import hub.jbl.common.lib.api.validation.DiscountOnPaymentTypeAPI;
import hub.jbl.common.lib.utils.JblPaths;
import hub.jbl.common.lib.utils.impl.JblPathsImpl;
import hub.jbl.common.services.JblTransactionManager;
import hub.jbl.common.services.JblTransactionManagerImpl;
import hub.jbl.common.session.RemoteDiscountSessionData;
import hub.jbl.common.session.RemoteDiscountSessionDataImpl;
import hub.jbl.common.spring.AppSpringConfig;
import hub.jbl.dao.*;
import hub.jbl.dao.common.QueryLoader;
import hub.jbl.dao.common.impl.pg.QueryLoaderImpl;
import hub.jbl.dao.impl.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

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

    @Bean
    public RestVerticle restVerticle() {
        return new RestVerticle();
    }

    @Qualifier("connectionPool")
    @Bean
    public JDBCClient jdbcClient(Vertx vertx, DataSource dataSource) {
        return JDBCClient.create(vertx, dataSource);
    }

    @Bean
    public QueryLoader queryLoader() {
        return new QueryLoaderImpl();
    }

    @Bean
    public DataSource dataSource(JsonObject config) {
        return new HikariDataSource(getHikariConfig(config));
    }


    protected HikariConfig getHikariConfig(JsonObject config) {
        return getHikariGenericConfig(config, "pg.", null);
    }

    @Bean
    public JblPaths jblPaths(JsonObject config) {
        return new JblPathsImpl(config);
    }


    protected HikariConfig getHikariGenericConfig(JsonObject config, String prefix, String sqLiteFIlePath) {

        String jdbcUrl = sqLiteFIlePath == null ? config.getString(prefix + "jdbc.jdbcUrl") + config.getString("environment.database", "") : "jdbc:sqlite:" + sqLiteFIlePath;

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(config.getString(prefix + "jdbc.poolName"));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(config.getString(prefix + "jdbc.maximumPoolSize")));
        hikariConfig.setMinimumIdle(Integer.parseInt(config.getString(prefix + "jdbc.minimumIdle")));
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getString(prefix + "jdbc.username"));
        hikariConfig.setPassword(getPwd(config, prefix));
        hikariConfig.setDriverClassName(config.getString(prefix + "jdbc.driverClassName"));
        hikariConfig.setConnectionTestQuery(config.getString(prefix + "jdbc.preferredTestquery"));
        hikariConfig.addDataSourceProperty("cachePrepStmts", config.getString(prefix + "jdbc.cachePrepStmts"));
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", config.getString(prefix + "jdbc.prepStmtCacheSize"));
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", config.getString(prefix + "jdbc.prepStmtCacheSqlLimit"));
        hikariConfig.addDataSourceProperty("useServerPrepStmts", config.getString(prefix + "jdbc.useServerPrepStmts"));
        hikariConfig.setLeakDetectionThreshold(Long.parseLong(config.getString(prefix + "jdbc.leakDetectionThreshold"))); //Out 2 seconds it prints merge log to detect leak
        hikariConfig.setConnectionTimeout(TimeUnit.SECONDS.toMillis(Long.parseLong(config.getString(prefix + "jdbc.connectionTimeout")))); //Property is defined in Seconds

        hikariConfig.setInitializationFailTimeout(config.getInteger(prefix + "jdbc.initializationFailTimeout"));

        if (!StringUtils.isEmpty(config.getString(prefix + "jdbc.maxLifeTime"))) {
            hikariConfig.setMaxLifetime(TimeUnit.MINUTES.toMillis(Long.parseLong(config.getString(prefix + "jdbc.maxLifeTime")))); //Property is defined in Minutes
        }

        if (!StringUtils.isEmpty(config.getString(prefix + "jdbc.idleTimeout"))) {
            hikariConfig.setIdleTimeout(TimeUnit.MINUTES.toMillis(Long.parseLong(config.getString(prefix + "jdbc.idleTimeout")))); //Property is defined in Minutes
        }

        return hikariConfig;
    }

    private String getPwd(JsonObject config, String prefix) {
        return CryptoPassword.get(config.getString("jbl.instance.identifier")).deCryptPassword(config.getString(prefix + "jdbc.password"));
    }
}
