package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Execution(ExecutionMode.CONCURRENT)
class FusionNodeControllerTest {

    private FusionNodeStatusService statusService;
    private FusionNodeController controller;

    @BeforeEach
    void setUp() {
        statusService = mock(FusionNodeStatusService.class);
        controller = new FusionNodeController(statusService);
    }

    @Test
    void status_delegatesToStatusService() {
        SseEmitter emitter = new SseEmitter();
        when(statusService.subscribe()).thenReturn(emitter);
        assertSame(emitter, controller.status());
        verify(statusService).subscribe();
    }

    @Test
    void probe_delegatesToStatusService() {
        controller.probe();
        verify(statusService).probe();
    }
}
