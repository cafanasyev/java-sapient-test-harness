package io.sapient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.sapient.transmission.INodeDispatcher;
import io.sapient.transmission.NodeDispatcher;
import io.sapient.transmission.NodeDispatcherConfig;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@Execution(ExecutionMode.CONCURRENT)
class SapientConfigTest {

    private INodeDispatcher dispatcher;

    @AfterEach
    void tearDown() throws Exception {
        if (dispatcher != null) {
            dispatcher.close();
        }
    }

    @Test
    void nodeDispatcher_returnsNodeDispatcher() {
        dispatcher = createDispatcher(UUID.randomUUID());
        assertInstanceOf(NodeDispatcher.class, dispatcher);
    }

    @Test
    void nodeDispatcher_configuresCorrectDestinationId() {
        UUID fusionNodeId = UUID.randomUUID();
        dispatcher = createDispatcher(fusionNodeId);
        NodeDispatcherConfig config =
                (NodeDispatcherConfig) ReflectionTestUtils.getField(dispatcher, "config");
        assertNotNull(config);
        assertEquals(fusionNodeId, config.destinationId());
    }

    @Test
    void nodeDispatcher_closeInvokedOnContextShutdown() throws Exception {
        INodeDispatcher dispatcher = mock(INodeDispatcher.class);
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(INodeDispatcher.class, () -> dispatcher);
            ctx.refresh();
        }
        verify(dispatcher).close();
    }

    private INodeDispatcher createDispatcher(UUID fusionNodeId) {
        SapientConfig config = new SapientConfig();
        ReflectionTestUtils.setField(config, "host", "localhost");
        ReflectionTestUtils.setField(config, "port", 9999);
        ReflectionTestUtils.setField(config, "fusionNodeId", fusionNodeId.toString());
        return config.nodeDispatcher(config.socketClient(config.plainSocketProvider()));
    }
}
