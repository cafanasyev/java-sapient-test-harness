package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

@Execution(ExecutionMode.CONCURRENT)
class NodeOrderingTest {

    private static final StatusReport STATUS =
            StatusReport.newBuilder().setSystem(StatusReport.System.SYSTEM_OK).build();

    private static EdgeNode node(UUID id, UUID... dependentIds) {
        Registration.Builder b = Registration.newBuilder();
        for (UUID d : dependentIds) b.addDependentNodes(d.toString());
        return new EdgeNode(id, b.build(), STATUS, true);
    }

    @Test
    void reorder_emptyInput_returnsEmpty() {
        assertTrue(NodeOrdering.reorder(List.of()).isEmpty());
    }

    @Test
    void reorder_noDependents_preservesOrderAndAllChildFalse() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<NodeOrdering.Row> rows = NodeOrdering.reorder(List.of(node(a), node(b)));
        assertEquals(List.of(a, b), rows.stream().map(r -> r.node().getNodeId()).toList());
        assertTrue(rows.stream().noneMatch(NodeOrdering.Row::child));
    }

    @Test
    void reorder_parentThenChild_emitsChildAfterParent() {
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        List<NodeOrdering.Row> rows =
                NodeOrdering.reorder(List.of(node(parent, child), node(child)));
        assertEquals(List.of(parent, child), rows.stream().map(r -> r.node().getNodeId()).toList());
        assertFalse(rows.get(0).child());
        assertTrue(rows.get(1).child());
    }

    @Test
    void reorder_childBeforeParent_inputOrder_stillEmitsChildAfterParent() {
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        List<NodeOrdering.Row> rows =
                NodeOrdering.reorder(List.of(node(child), node(parent, child)));
        assertEquals(List.of(parent, child), rows.stream().map(r -> r.node().getNodeId()).toList());
        assertFalse(rows.get(0).child());
        assertTrue(rows.get(1).child());
    }

    @Test
    void reorder_dependentNotInRegistry_isIgnored() {
        UUID parent = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        List<NodeOrdering.Row> rows = NodeOrdering.reorder(List.of(node(parent, missing)));
        assertEquals(List.of(parent), rows.stream().map(r -> r.node().getNodeId()).toList());
        assertFalse(rows.get(0).child());
    }

    @Test
    void reorder_invalidUuidInDependents_isIgnored() {
        UUID parent = UUID.randomUUID();
        Registration r = Registration.newBuilder().addDependentNodes("not-a-uuid").build();
        EdgeNode p = new EdgeNode(parent, r, STATUS, true);
        List<NodeOrdering.Row> rows = NodeOrdering.reorder(List.of(p));
        assertEquals(1, rows.size());
        assertEquals(parent, rows.get(0).node().getNodeId());
        assertFalse(rows.get(0).child());
    }

    @Test
    void reorder_sameChildClaimedByTwoParents_firstParentWins() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        List<NodeOrdering.Row> rows =
                NodeOrdering.reorder(List.of(node(p1, child), node(p2, child), node(child)));
        assertEquals(List.of(p1, child, p2), rows.stream().map(r -> r.node().getNodeId()).toList());
        assertFalse(rows.get(0).child());
        assertTrue(rows.get(1).child());
        assertFalse(rows.get(2).child());
    }

    @Test
    void reorder_parentWithMultipleChildren_emitsInDeclaredOrder() {
        UUID parent = UUID.randomUUID();
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID c3 = UUID.randomUUID();
        List<NodeOrdering.Row> rows =
                NodeOrdering.reorder(
                        List.of(node(parent, c2, c3, c1), node(c1), node(c2), node(c3)));
        assertEquals(
                List.of(parent, c2, c3, c1), rows.stream().map(r -> r.node().getNodeId()).toList());
        assertFalse(rows.get(0).child());
        assertTrue(rows.get(1).child());
        assertTrue(rows.get(2).child());
        assertTrue(rows.get(3).child());
    }

    @Test
    void reorder_nodeWithNullRegistration_treatedAsRootWithNoDependents() {
        UUID id = UUID.randomUUID();
        EdgeNode n = new EdgeNode(id, null, STATUS, true);
        List<NodeOrdering.Row> rows = NodeOrdering.reorder(List.of(n));
        assertEquals(1, rows.size());
        assertEquals(id, rows.get(0).node().getNodeId());
        assertFalse(rows.get(0).child());
    }
}
