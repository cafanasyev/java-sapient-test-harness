package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sapient.transport.ConnectionState;
import io.sapient.transport.IClient;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.ITemplateEngine;

@Execution(ExecutionMode.CONCURRENT)
class FusionNodeStatusServiceTest {

    private IClient client;
    private FusionNodeStatusService service;

    @BeforeEach
    void setUp() {
        client = mock(IClient.class);
        when(client.getState()).thenReturn(ConnectionState.CONNECTED);
        service = new FusionNodeStatusService(client, mock(ITemplateEngine.class));
    }

    @Test
    void subscribe_returnsEmitter() {
        assertNotNull(service.subscribe());
    }

    @Test
    void constructor_doesNotProbeWhenInitialStateIsConnected() {
        verify(client, never()).probeReachable(any());
    }

    @Test
    void constructor_probesWhenInitialStateIsNotConnected() {
        IClient newClient = mock(IClient.class);
        when(newClient.getState()).thenReturn(ConnectionState.DISCONNECTED);
        when(newClient.probeReachable(any())).thenReturn(false);
        new FusionNodeStatusService(newClient, mock(ITemplateEngine.class));
        verify(newClient).probeReachable(any());
    }

    @Test
    void onContextClosed_completesEmittersAndRemovesListener() {
        service.subscribe();
        service.onContextClosed();
        verify(client).removeStateChangeListener(any());
    }

    @Test
    void probe_updatesLastReachableAndProbes() {
        when(client.probeReachable(any())).thenReturn(false);
        service.probe();
        verify(client).probeReachable(any());
    }

    @Test
    void onStateChange_probesWhenDisconnected() {
        when(client.probeReachable(any())).thenReturn(true);
        stateChangeListener().accept(ConnectionState.DISCONNECTED, Instant.now());
        verify(client).probeReachable(any());
    }

    @Test
    void onStateChange_doesNotProbeWhenConnected() {
        stateChangeListener().accept(ConnectionState.CONNECTED, Instant.now());
        verify(client, never()).probeReachable(any());
    }

    @Test
    void onStateChange_doesNotProbeWhenConnecting() {
        stateChangeListener().accept(ConnectionState.CONNECTING, Instant.now());
        verify(client, never()).probeReachable(any());
    }

    @Test
    void onStateChange_doesNotProbeWhenClosed() {
        stateChangeListener().accept(ConnectionState.CLOSED, Instant.now());
        verify(client, never()).probeReachable(any());
    }

    @Test
    void connectionCssClass_connected() {
        assertEquals(
                "badge badge-connected",
                FusionNodeStatusService.connectionCssClass(ConnectionState.CONNECTED));
    }

    @Test
    void connectionCssClass_connecting() {
        assertEquals(
                "badge badge-connecting",
                FusionNodeStatusService.connectionCssClass(ConnectionState.CONNECTING));
    }

    @Test
    void connectionCssClass_disconnected() {
        assertEquals(
                "badge badge-disconnected",
                FusionNodeStatusService.connectionCssClass(ConnectionState.DISCONNECTED));
    }

    @Test
    void connectionCssClass_closed() {
        assertEquals(
                "badge badge-closed",
                FusionNodeStatusService.connectionCssClass(ConnectionState.CLOSED));
    }

    @Test
    void connectionLabel_connected() {
        assertEquals(
                "Connected", FusionNodeStatusService.connectionLabel(ConnectionState.CONNECTED));
    }

    @Test
    void connectionLabel_connecting() {
        assertEquals(
                "Connecting\u2026",
                FusionNodeStatusService.connectionLabel(ConnectionState.CONNECTING));
    }

    @Test
    void connectionLabel_disconnected() {
        assertEquals(
                "Disconnected",
                FusionNodeStatusService.connectionLabel(ConnectionState.DISCONNECTED));
    }

    @Test
    void connectionLabel_closed() {
        assertEquals("Closed", FusionNodeStatusService.connectionLabel(ConnectionState.CLOSED));
    }

    @Test
    void reachabilityCssClass_reachable() {
        assertEquals("badge badge-reachable", FusionNodeStatusService.reachabilityCssClass(true));
    }

    @Test
    void reachabilityCssClass_unreachable() {
        assertEquals(
                "badge badge-unreachable", FusionNodeStatusService.reachabilityCssClass(false));
    }

    @Test
    void reachabilityLabel_reachable() {
        assertEquals("Reachable", FusionNodeStatusService.reachabilityLabel(true));
    }

    @Test
    void reachabilityLabel_unreachable() {
        assertEquals("Unreachable", FusionNodeStatusService.reachabilityLabel(false));
    }

    @SuppressWarnings("unchecked")
    private BiConsumer<ConnectionState, Instant> stateChangeListener() {
        ArgumentCaptor<BiConsumer<ConnectionState, Instant>> captor =
                ArgumentCaptor.forClass(BiConsumer.class);
        verify(client).addStateChangeListener(captor.capture());
        return captor.getValue();
    }
}
