package io.sapient;

import io.sapient.transmission.INodeDispatcher;
import io.sapient.transmission.NodeDispatcher;
import io.sapient.transmission.NodeDispatcherConfig;
import io.sapient.transport.IClient;
import io.sapient.transport.SocketClient;
import io.sapient.transport.SocketProvider;
import io.sapient.transport.SslContextFactory;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Value("${fusion-node.tls.ca-cert}")
    private String caCertPath;

    @Value("${fusion-node.tls.client-cert}")
    private String clientCertPath;

    @Value("${fusion-node.tls.client-key}")
    private String clientKeyPath;

    @Bean
    @ConditionalOnProperty(name = "fusion-node.tls.enabled", havingValue = "true")
    SocketProvider tlsSocketProvider() throws Exception {
        byte[] caCert = Files.readAllBytes(Paths.get(caCertPath));
        byte[] clientCert = Files.readAllBytes(Paths.get(clientCertPath));
        byte[] clientKey = Files.readAllBytes(Paths.get(clientKeyPath));
        SSLContext sslContext = new SslContextFactory().create(clientKey, clientCert, caCert);
        return new SocketProvider() {
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
    SocketProvider plainSocketProvider() {
        return new SocketProvider() {
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
    IClient socketClient(SocketProvider socketProvider) {
        return new SocketClient(socketProvider);
    }

    @Bean
    INodeDispatcher nodeDispatcher(IClient client) {
        NodeDispatcherConfig config = NodeDispatcherConfig.defaults(UUID.fromString(fusionNodeId));
        return new NodeDispatcher(client, config);
    }
}
