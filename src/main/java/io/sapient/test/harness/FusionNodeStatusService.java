package io.sapient.test.harness;

import io.sapient.transport.ConnectionState;
import io.sapient.transport.IClient;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Component
class FusionNodeStatusService {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    private final IClient client;
    private final ITemplateEngine templateEngine;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final BiConsumer<ConnectionState, Instant> stateChangeListener = this::onStateChange;
    private volatile ConnectionState lastState;
    private volatile boolean lastReachable;

    FusionNodeStatusService(IClient client, ITemplateEngine templateEngine) {
        this.client = client;
        this.templateEngine = templateEngine;
        lastState = client.getState();
        lastReachable =
                lastState == ConnectionState.CONNECTED || client.probeReachable(PROBE_TIMEOUT);
        client.addStateChangeListener(stateChangeListener);
    }

    @EventListener(ContextClosedEvent.class)
    void onContextClosed() {
        client.removeStateChangeListener(stateChangeListener);
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }

    SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        sendTo(emitter, "connection", renderConnection(lastState));
        sendTo(emitter, "reachability", renderReachability(lastReachable));
        return emitter;
    }

    void probe() {
        boolean reachable = client.probeReachable(PROBE_TIMEOUT);
        lastReachable = reachable;
        broadcastEvent("reachability", renderReachability(reachable));
    }

    private void onStateChange(ConnectionState state, Instant ts) {
        lastState = state;
        broadcastEvent("connection", renderConnection(state));
        if (state == ConnectionState.CONNECTED) {
            lastReachable = true;
            broadcastEvent("reachability", renderReachability(true));
        } else if (state == ConnectionState.DISCONNECTED) {
            boolean reachable = client.probeReachable(PROBE_TIMEOUT);
            lastReachable = reachable;
            broadcastEvent("reachability", renderReachability(reachable));
        }
    }

    private String renderConnection(ConnectionState state) {
        Context ctx = new Context();
        ctx.setVariable("cssClass", connectionCssClass(state));
        ctx.setVariable("label", connectionLabel(state));
        return templateEngine.process("fusion-node", Set.of("connection"), ctx);
    }

    private String renderReachability(boolean reachable) {
        Context ctx = new Context();
        ctx.setVariable("cssClass", reachabilityCssClass(reachable));
        ctx.setVariable("label", reachabilityLabel(reachable));
        return templateEngine.process("fusion-node", Set.of("reachability"), ctx);
    }

    private void broadcastEvent(String eventName, String html) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(html));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    private static void sendTo(SseEmitter emitter, String eventName, String html) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(html));
        } catch (IOException e) {
            log.debug("failed to send initial state to new subscriber", e);
        }
    }

    static String connectionCssClass(ConnectionState state) {
        return "badge badge-" + state.name().toLowerCase();
    }

    static String connectionLabel(ConnectionState state) {
        return switch (state) {
            case CONNECTED -> "Connected";
            case CONNECTING -> "Connecting\u2026";
            case DISCONNECTED -> "Disconnected";
            case CLOSED -> "Closed";
        };
    }

    static String reachabilityCssClass(boolean reachable) {
        return reachable ? "badge badge-reachable" : "badge badge-unreachable";
    }

    static String reachabilityLabel(boolean reachable) {
        return reachable ? "Reachable" : "Unreachable";
    }
}
