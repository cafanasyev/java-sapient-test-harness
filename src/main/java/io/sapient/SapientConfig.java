package io.sapient;

import io.sapient.transmission.INodeDispatcher;
import io.sapient.transmission.NodeDispatcher;
import io.sapient.transmission.NodeDispatcherConfig;
import io.sapient.transport.IClient;
import io.sapient.transport.SocketClient;
import io.sapient.transport.SocketProvider;
import java.net.Socket;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    INodeDispatcher nodeDispatcher() {
        SocketProvider provider = () -> new Socket(host, port);
        IClient client = new SocketClient(provider);
        NodeDispatcherConfig config = NodeDispatcherConfig.defaults(UUID.fromString(fusionNodeId));
        NodeDispatcher dispatcher = new NodeDispatcher(client, config);
        Thread.ofVirtual().name("node-dispatcher").start(dispatcher);
        return dispatcher;
    }
}
