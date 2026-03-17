package io.sapient.test.harness;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.sapient.transmission.INodeDispatcher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

@RestController
@RequestMapping("/nodes")
@RequiredArgsConstructor
class NodeController {

    private static final Duration PUBLISH_TIMEOUT = Duration.ofSeconds(5);

    private final EdgeNodeRegistry registry;
    private final INodeDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    @GetMapping
    List<NodeView> list() {
        return registry.getNodes().stream().map(n -> toView((EdgeNode) n)).toList();
    }

    @PostMapping("/reload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reload() {
        registry.reload();
    }

    @PutMapping("/{id}/online/{value}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setOnline(@PathVariable UUID id, @PathVariable boolean value) {
        registry.setOnline(id, value);
    }

    @PutMapping("/auto-reload-on-manual-send/{value}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setAutoReloadOnManualSend(@PathVariable boolean value) {
        registry.setAutoReloadOnManualSend(value);
    }

    @PostMapping("/{id}/registration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sendRegistration(@PathVariable UUID id) throws TimeoutException, InterruptedException {
        EdgeNode node =
                (EdgeNode)
                        registry.getNode(id)
                                .orElseThrow(
                                        () -> new NoSuchElementException("Node not found: " + id));
        if (registry.isAutoReloadOnManualSend()) {
            registry.reload();
        }
        Registration registration =
                Optional.ofNullable(node.getRegistration())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "No registration.json for node: " + id));
        dispatcher.publish(registration, id, PUBLISH_TIMEOUT);
    }

    @PostMapping("/{id}/status-report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sendStatusReport(@PathVariable UUID id) throws TimeoutException, InterruptedException {
        EdgeNode node =
                (EdgeNode)
                        registry.getNode(id)
                                .orElseThrow(
                                        () -> new NoSuchElementException("Node not found: " + id));
        if (registry.isAutoReloadOnManualSend()) {
            registry.reload();
        }
        StatusReport statusReport =
                Optional.ofNullable(node.getStatusReport())
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "No status_report.json for node: " + id));
        dispatcher.publish(statusReport, id, PUBLISH_TIMEOUT);
    }

    @PostMapping("/{id}/alert")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sendAlert(@PathVariable UUID id) throws TimeoutException, InterruptedException {
        EdgeNode node =
                (EdgeNode)
                        registry.getNode(id)
                                .orElseThrow(
                                        () -> new NoSuchElementException("Node not found: " + id));
        if (registry.isAutoReloadOnManualSend()) {
            registry.reload();
        }
        Alert alert =
                node.getAlert()
                        .orElseThrow(
                                () -> new NoSuchElementException("No alert.json for node: " + id));
        dispatcher.publish(alert, id, PUBLISH_TIMEOUT);
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    private NodeView toView(EdgeNode node) {
        return new NodeView(
                node.getNodeId(),
                node.isOnline(),
                node.hasRegistration(),
                node.hasStatusReport(),
                node.hasAlert(),
                protoToJson(node.getRegistration()),
                protoToJson(node.getStatusReport()));
    }

    private JsonNode protoToJson(MessageOrBuilder msg) {
        try {
            return objectMapper.readTree(JsonFormat.printer().print(msg));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record NodeView(
            UUID nodeId,
            boolean online,
            boolean hasRegistration,
            boolean hasStatusReport,
            boolean hasAlert,
            JsonNode registration,
            JsonNode statusReport) {}
}
