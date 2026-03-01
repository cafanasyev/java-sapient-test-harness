package io.sapient.test.harness;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/nodes")
@RequiredArgsConstructor
class NodeController {

    private final EdgeNodeRegistry registry;
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

    @PutMapping("/{id}/online")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setOnline(@PathVariable UUID id, @RequestBody boolean online) {
        registry.setOnline(id, online);
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    private NodeView toView(EdgeNode node) {
        return new NodeView(
                node.getNodeId(),
                node.isOnline(),
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

    record NodeView(UUID nodeId, boolean online, JsonNode registration, JsonNode statusReport) {}
}
