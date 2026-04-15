package io.sapient.test.harness;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
class FusionNodeController {

    private final FusionNodeStatusService statusService;

    @GetMapping("/fusion-node/status")
    SseEmitter status() {
        return statusService.subscribe();
    }

    @PostMapping("/fusion-node/probe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void probe() {
        statusService.probe();
    }
}
