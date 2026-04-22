package io.sapient.test.harness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;

final class NodeOrdering {

    record Row(EdgeNode node, boolean child) {}

    private NodeOrdering() {}

    static List<Row> reorder(Collection<EdgeNode> nodes) {
        Map<UUID, EdgeNode> byId = new HashMap<>();
        for (EdgeNode n : nodes) byId.put(n.getNodeId(), n);

        Map<UUID, UUID> childOf = new HashMap<>();
        for (EdgeNode p : nodes)
            for (UUID c : dependents(p))
                if (byId.containsKey(c)) childOf.putIfAbsent(c, p.getNodeId());

        List<Row> rows = new ArrayList<>(nodes.size());
        for (EdgeNode n : nodes) {
            if (childOf.containsKey(n.getNodeId())) continue;
            rows.add(new Row(n, false));
            for (UUID c : dependents(n))
                if (n.getNodeId().equals(childOf.get(c))) rows.add(new Row(byId.get(c), true));
        }
        return rows;
    }

    private static List<UUID> dependents(EdgeNode n) {
        Registration r = n.getRegistration();
        if (r == null) return List.of();
        List<UUID> out = new ArrayList<>(r.getDependentNodesCount());
        for (String s : r.getDependentNodesList()) {
            try {
                out.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }
}
