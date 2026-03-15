package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sapient.transmission.INode;
import io.sapient.transmission.INodeDispatcher;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

@Execution(ExecutionMode.CONCURRENT)
class EdgeNodeRegistryTest {

    private EdgeNodeLoader loader;
    private INodeDispatcher dispatcher;
    private EdgeNodeRegistry registry;

    @BeforeEach
    void setUp() {
        loader = mock(EdgeNodeLoader.class);
        dispatcher = mock(INodeDispatcher.class);
        registry = new EdgeNodeRegistry(loader, dispatcher);
        ReflectionTestUtils.setField(registry, "baseDir", ".");
    }

    @Test
    void init_loadsAndRegistersNodesOnStartup() throws IOException {
        EdgeNode node = node(UUID.randomUUID());
        when(loader.load(any())).thenReturn(List.of(node));
        registry.init();
        assertEquals(1, registry.getNodes().size());
        verify(dispatcher).register(node);
    }

    @Test
    void reload_registersNewNode() throws IOException {
        EdgeNode node = node(UUID.randomUUID());
        when(loader.load(any())).thenReturn(List.of(node));
        registry.reload();
        verify(dispatcher).register(node);
    }

    @Test
    void reload_updatesExistingNodeInPlace_withoutReregistering() throws IOException {
        UUID id = UUID.randomUUID();
        Registration reg1 = Registration.newBuilder().setIcdVersion("v1").build();
        Registration reg2 = Registration.newBuilder().setIcdVersion("v2").build();
        StatusReport sr1 = StatusReport.newBuilder().setMode("mode1").setReportId("sr1").build();
        StatusReport sr2 = StatusReport.newBuilder().setMode("mode2").setReportId("sr2").build();
        when(loader.load(any()))
                .thenReturn(List.of(new EdgeNode(id, reg1, sr1, true)))
                .thenReturn(List.of(new EdgeNode(id, reg2, sr2, true)));

        registry.reload();
        INode original = registry.getNodes().iterator().next();

        registry.reload();
        INode updated = registry.getNodes().iterator().next();

        assertSame(original, updated);
        assertEquals(reg2, updated.getRegistration());
        assertEquals(sr2, updated.getStatusReport());
        verify(dispatcher).register(any());
        verify(dispatcher, never()).unregister(any());
    }

    @Test
    void reload_unregistersNode_whenNoLongerLoaded() throws IOException {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        EdgeNode node1 = node(id1);
        EdgeNode node2 = node(id2);
        when(loader.load(any())).thenReturn(List.of(node1, node2)).thenReturn(List.of(node1));

        registry.reload();
        registry.reload();

        assertEquals(1, registry.getNodes().size());
        verify(dispatcher).unregister(node2);
    }

    @Test
    void reload_doesNotThrow_onIOException() throws IOException {
        when(loader.load(any())).thenThrow(new IOException("disk error"));
        assertDoesNotThrow(() -> registry.reload());
    }

    @Test
    void getNodes_returnsUnmodifiableCollection() throws IOException {
        when(loader.load(any())).thenReturn(List.of(node(UUID.randomUUID())));
        registry.reload();
        Collection<INode> nodes = registry.getNodes();
        assertThrows(UnsupportedOperationException.class, nodes::clear);
    }

    @Test
    void setOnline_updatesNode() throws IOException {
        UUID id = UUID.randomUUID();
        EdgeNode node = node(id);
        when(loader.load(any())).thenReturn(List.of(node));
        registry.reload();

        registry.setOnline(id, false);

        assertFalse(node.isOnline());
    }

    @Test
    void setOnline_throwsNoSuchElement_whenNodeAbsent() {
        assertThrows(
                NoSuchElementException.class, () -> registry.setOnline(UUID.randomUUID(), true));
    }

    private EdgeNode node(UUID id) {
        return new EdgeNode(
                id, Registration.getDefaultInstance(), StatusReport.getDefaultInstance(), true);
    }
}
