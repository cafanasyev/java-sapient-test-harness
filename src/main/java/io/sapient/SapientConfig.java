package io.sapient;

import io.sapient.transmission.INodeDispatcher;
import io.sapient.transmission.NodeDispatcher;
import io.sapient.transmission.NodeDispatcherConfig;
import io.sapient.transport.IClient;
import io.sapient.transport.ISocketProvider;
import io.sapient.transport.SocketClient;
import io.sapient.transport.SslContextFactory;
import io.sapient.transport.health.HealthCheckConfig;
import io.sapient.transport.health.HealthCheckType;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SapientConfig {

    @Value("${fusion-node.host}")
    private String host;

    @Value("${fusion-node.port}")
    private int port;

    @Value("${fusion-node.id}")
    private String fusionNodeId;

    @Value("${fusion-node.tls.ca-cert:}")
    private String caCertPath;

    @Value("${fusion-node.tls.client-cert:}")
    private String clientCertPath;

    @Value("${fusion-node.tls.client-key:}")
    private String clientKeyPath;

    @Value("${fusion-node.health-check.type}")
    private HealthCheckType healthCheckType;

    @Value("${fusion-node.health-check.interval}")
    private Duration healthCheckInterval;

    @Value("${fusion-node.health-check.timeout}")
    private Duration healthCheckTimeout;

    @Value("${fusion-node.health-check.failure-threshold}")
    private int healthCheckFailureThreshold;

    @Value("${fusion-node.socket.initial-reconnect-delay:1s}")
    private Duration socketInitialReconnectDelay;

    @Bean
    @ConditionalOnProperty(name = "fusion-node.tls.enabled", havingValue = "true")
    ISocketProvider tlsSocketProvider() throws Exception {
        byte[] caCert = Files.readAllBytes(Paths.get(caCertPath));
        byte[] clientCert = Files.readAllBytes(Paths.get(clientCertPath));
        byte[] clientKey = Files.readAllBytes(Paths.get(clientKeyPath));
        SSLContext sslContext = new SslContextFactory().create(clientKey, clientCert, caCert);
        return new ISocketProvider() {
            @Override
            public Socket get() throws IOException {
                return sslContext.getSocketFactory().createSocket(host, port);
            }

            @Override
            public String host() {
                return host;
            }

            @Override
            public int port() {
                return port;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(
            name = "fusion-node.tls.enabled",
            havingValue = "false",
            matchIfMissing = true)
    ISocketProvider plainSocketProvider() {
        return new ISocketProvider() {
            @Override
            public Socket get() throws IOException {
                return new Socket(host, port);
            }

            @Override
            public String host() {
                return host;
            }

            @Override
            public int port() {
                return port;
            }
        };
    }

    @Bean
    IClient socketClient(ISocketProvider socketProvider) {
        HealthCheckConfig healthCheck =
                new HealthCheckConfig(
                        healthCheckType,
                        healthCheckInterval,
                        healthCheckTimeout,
                        healthCheckFailureThreshold);
        return new SocketClient(socketProvider, healthCheck, socketInitialReconnectDelay);
    }

    @Bean
    INodeDispatcher nodeDispatcher(IClient client) {
        UUID fusionNodeId = UUID.fromString(this.fusionNodeId);
        // the dispatcher reads the connection-loss detection delay from the client itself
        return new NodeDispatcher(client, NodeDispatcherConfig.defaults(fusionNodeId));
    }
}
