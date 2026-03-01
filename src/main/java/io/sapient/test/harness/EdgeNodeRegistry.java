package io.sapient.test.harness;

import io.sapient.transmission.INode;
import io.sapient.transmission.INodeDispatcher;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeNodeRegistry {

    private final EdgeNodeLoader loader;
    private final INodeDispatcher dispatcher;

    @Value("${edge-node.loader.base-dir}")
    private String baseDir;

    private final ConcurrentMap<UUID, EdgeNode> nodes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        try {
            List<INode> loaded = loader.load(Path.of(baseDir));
            Set<UUID> loadedIds = new HashSet<>();
            for (INode iNode : loaded) {
                EdgeNode fresh = (EdgeNode) iNode;
                loadedIds.add(fresh.getNodeId());
                EdgeNode result =
                        nodes.merge(
                                fresh.getNodeId(),
                                fresh,
                                (existing, replacement) -> {
                                    existing.setRegistration(replacement.getRegistration());
                                    existing.setStatusReport(replacement.getStatusReport());
                                    return existing;
                                });
                if (result == fresh) {
                    dispatcher.register(fresh);
                }
            }
            Set<UUID> toRemove = new HashSet<>(nodes.keySet());
            toRemove.removeAll(loadedIds);
            for (UUID id : toRemove) {
                EdgeNode removed = nodes.remove(id);
                if (removed != null) {
                    dispatcher.unregister(removed);
                }
            }
            log.debug("Reloaded {} edge node(s) from {}", nodes.size(), baseDir);
        } catch (IOException e) {
            log.error("Failed to reload edge nodes from {}", baseDir, e);
        }
    }

    public Collection<INode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public void setOnline(UUID id, boolean online) throws NoSuchElementException {
        EdgeNode node = nodes.get(id);
        if (node == null) throw new NoSuchElementException("Node not found: " + id);
        node.setOnline(online);
    }
}
