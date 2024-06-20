package hub.ebb.jblcluster;

import com.hazelcast.config.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.ConfigUtil;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Launcher {

    private static final String CURRENT_IP = "currentIp";
    static Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {

        String path = getConfigFilePath(args);

        JsonObject config = loadConfigJson(path);

        Config clusterConfiguration = getClusterConfiguration(config);
        final VertxOptions vertxOptions = new VertxOptions().setClusterManager(new HazelcastClusterManager(clusterConfiguration));
        vertxOptions.getEventBusOptions().setHost(config.getString(CURRENT_IP)).setClusterPublicHost(config.getString(CURRENT_IP));
        Vertx.clusteredVertx(vertxOptions, vertxAsyncResult -> {
            if (vertxAsyncResult.succeeded()) {
                Objects.requireNonNull(vertxAsyncResult.result()).deployVerticle(new DeployerVerticle(), new DeploymentOptions().setConfig(config));
            } else {
                throw new RuntimeException(vertxAsyncResult.cause());
            }
        });
    }

    private static String getConfigFilePath(String[] args) {
        return "./module-config.json";
    }

    private static JsonObject loadConfigJson(String path) {
        JsonObject conf;
        try (Scanner scanner = new Scanner(new File(path), StandardCharsets.UTF_8).useDelimiter("\\A")) {
            String sconf = scanner.next();
            try {
                conf = new JsonObject(sconf);
            } catch (DecodeException e) {
                logger.error("Configuration file " + sconf + " does not contain a valid JSON object", e);
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException fnfe) {
            logger.error("Cannot find file " + path + ". I'll create it");
            conf = new JsonObject();
            conf.put(CURRENT_IP, "127.0.0.1");
            conf.put("jblIp", "127.0.0.1");
            conf.put("clusterName", "change-it");
            conf.put("moduleName", "module.name.vertx");
            try {
                Files.write(Path.of(path), conf.encodePrettily().getBytes(StandardCharsets.UTF_8)).toString();
            } catch (IOException e) {
                logger.error("Error creating config file", e);
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            logger.error("Error loading config file", e);
            throw new RuntimeException(e);
        }
        return conf;
    }

    private static Config getClusterConfiguration(JsonObject configJson) {

        Config config = ConfigUtil.loadConfig();
        config.setProperty("hazelcast.logging.type", "log4j2");
        config.setProperty("hazelcast.socket.bind.any", "false");
        config.setClusterName(configJson.getString("clusterName"));

        final List<String> ips = Arrays.asList(configJson.getString("jblIp"));

        TcpIpConfig tcpIpConfig = new TcpIpConfig().setMembers(ips).setEnabled(true);
        MulticastConfig multicastConfig = new MulticastConfig().setEnabled(false);
        AwsConfig awsConfig = new AwsConfig().setEnabled(false);
        GcpConfig gcpConfig = new GcpConfig().setEnabled(false);
        AzureConfig azureConfig = new AzureConfig().setEnabled(false);
        KubernetesConfig kubernetesConfig = new KubernetesConfig().setEnabled(false);
        EurekaConfig eurekaConfig = new EurekaConfig().setEnabled(false);

        config.getNetworkConfig().setJoin(new JoinConfig().setTcpIpConfig(tcpIpConfig).setMulticastConfig(multicastConfig).setAwsConfig(awsConfig).setGcpConfig(gcpConfig).setAzureConfig(azureConfig).setKubernetesConfig(kubernetesConfig).setEurekaConfig(eurekaConfig));

        return config;
    }

}