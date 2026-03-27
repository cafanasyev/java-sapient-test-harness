package io.sapient.test.harness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sapient.transmission.INodeDispatcher;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.DetectionReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

@Execution(ExecutionMode.CONCURRENT)
class NodeControllerTest {

    private EdgeNodeRegistry registry;
    private INodeDispatcher dispatcher;
    private MockMvc mvc;

    private static final UUID NODE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        registry = mock(EdgeNodeRegistry.class);
        dispatcher = mock(INodeDispatcher.class);
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new NodeController(registry, dispatcher, new ObjectMapper()))
                        .build();
    }

    @Test
    void list_returnsEmptyArray_whenNoNodes() throws Exception {
        when(registry.getNodes()).thenReturn(List.of());

        mvc.perform(get("/nodes")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void list_returnsNodeWithOnlineAndProtoFields() throws Exception {
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.newBuilder().setIcdVersion("BSI Flex 335 v2.0").build(),
                        StatusReport.newBuilder().setMode("default").build(),
                        true);
        when(registry.getNodes()).thenReturn(List.of(node));

        mvc.perform(get("/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nodeId").value(NODE_ID.toString()))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[0].registration.icdVersion").value("BSI Flex 335 v2.0"))
                .andExpect(jsonPath("$[0].statusReport.mode").value("default"));
    }

    @Test
    void list_reflectsOnlineFalse() throws Exception {
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        false);
        when(registry.getNodes()).thenReturn(List.of(node));

        mvc.perform(get("/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].online").value(false));
    }

    @Test
    void setOnline_returns204_whenNodeExists() throws Exception {
        doNothing().when(registry).setOnline(NODE_ID, false);

        mvc.perform(put("/nodes/{id}/online/{value}", NODE_ID, false))
                .andExpect(status().isNoContent());
    }

    @Test
    void reload_returns204_andDelegatesToRegistry() throws Exception {
        mvc.perform(post("/nodes/reload")).andExpect(status().isNoContent());
        verify(registry).reload();
    }

    @Test
    void setOnline_returns404_whenNodeNotFound() throws Exception {
        doThrow(new NoSuchElementException()).when(registry).setOnline(NODE_ID, true);

        mvc.perform(put("/nodes/{id}/online/{value}", NODE_ID, true))
                .andExpect(status().isNotFound());
    }

    @Test
    void setAutoReloadOnManualSend_returns204() throws Exception {
        mvc.perform(put("/nodes/auto-reload-on-manual-send/{value}", true))
                .andExpect(status().isNoContent());
        verify(registry).setAutoReloadOnManualSend(true);
    }

    @Test
    void sendRegistration_reloadsFixtures_whenAutoReloadEnabled() throws Exception {
        Registration registration =
                Registration.newBuilder().setIcdVersion("BSI Flex 335 v2.0").build();
        EdgeNode node =
                new EdgeNode(NODE_ID, registration, StatusReport.getDefaultInstance(), true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        when(registry.isAutoReloadOnManualSend()).thenReturn(true);
        doNothing().when(dispatcher).publish(eq(registration), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/registration", NODE_ID)).andExpect(status().isNoContent());
        verify(registry).reload();
    }

    @Test
    void sendStatusReport_reloadsFixtures_whenAutoReloadEnabled() throws Exception {
        StatusReport statusReport =
                StatusReport.newBuilder().setMode("default").setReportId("r1").build();
        EdgeNode node =
                new EdgeNode(NODE_ID, Registration.getDefaultInstance(), statusReport, true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        when(registry.isAutoReloadOnManualSend()).thenReturn(true);
        doNothing().when(dispatcher).publish(eq(statusReport), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/status-report", NODE_ID)).andExpect(status().isNoContent());
        verify(registry).reload();
    }

    @Test
    void sendAlert_reloadsFixtures_whenAutoReloadEnabled() throws Exception {
        Alert alert = Alert.newBuilder().setAlertId("a1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        alert,
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        when(registry.isAutoReloadOnManualSend()).thenReturn(true);
        doNothing().when(dispatcher).publish(eq(alert), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/alert", NODE_ID)).andExpect(status().isNoContent());
        verify(registry).reload();
    }

    @Test
    void sendRegistration_doesNotReload_whenAutoReloadDisabled() throws Exception {
        Registration registration =
                Registration.newBuilder().setIcdVersion("BSI Flex 335 v2.0").build();
        EdgeNode node =
                new EdgeNode(NODE_ID, registration, StatusReport.getDefaultInstance(), true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        doNothing().when(dispatcher).publish(eq(registration), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/registration", NODE_ID)).andExpect(status().isNoContent());
        verify(registry, never()).reload();
    }

    @Test
    void sendRegistration_returns204_whenRegistrationPresent() throws Exception {
        Registration registration =
                Registration.newBuilder().setIcdVersion("BSI Flex 335 v2.0").build();
        EdgeNode node =
                new EdgeNode(NODE_ID, registration, StatusReport.getDefaultInstance(), true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        doNothing().when(dispatcher).publish(eq(registration), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/registration", NODE_ID)).andExpect(status().isNoContent());
        verify(dispatcher).publish(eq(registration), eq(NODE_ID), any());
    }

    @Test
    void sendRegistration_returns404_whenNodeNotFound() throws Exception {
        when(registry.getNode(NODE_ID)).thenReturn(Optional.empty());

        mvc.perform(post("/nodes/{id}/registration", NODE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void sendStatusReport_returns204_whenStatusReportPresent() throws Exception {
        StatusReport statusReport =
                StatusReport.newBuilder().setMode("default").setReportId("r1").build();
        EdgeNode node =
                new EdgeNode(NODE_ID, Registration.getDefaultInstance(), statusReport, true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        doNothing().when(dispatcher).publish(eq(statusReport), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/status-report", NODE_ID)).andExpect(status().isNoContent());
        verify(dispatcher).publish(eq(statusReport), eq(NODE_ID), any());
    }

    @Test
    void sendStatusReport_returns404_whenNodeNotFound() throws Exception {
        when(registry.getNode(NODE_ID)).thenReturn(Optional.empty());

        mvc.perform(post("/nodes/{id}/status-report", NODE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void sendAlert_returns204_whenAlertPresent() throws Exception {
        Alert alert = Alert.newBuilder().setAlertId("a1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        alert,
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        doNothing().when(dispatcher).publish(eq(alert), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/alert", NODE_ID)).andExpect(status().isNoContent());
        verify(dispatcher).publish(eq(alert), eq(NODE_ID), any());
    }

    @Test
    void sendAlert_returns404_whenNodeNotFound() throws Exception {
        when(registry.getNode(NODE_ID)).thenReturn(Optional.empty());

        mvc.perform(post("/nodes/{id}/alert", NODE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void sendAlert_returns404_whenNoAlertJson() throws Exception {
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));

        mvc.perform(post("/nodes/{id}/alert", NODE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void sendDetectionReport_returns204_whenDetectionReportPresent() throws Exception {
        DetectionReport dr =
                DetectionReport.newBuilder().setReportId("dr-1").setObjectId("obj-1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        null,
                        dr,
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        doNothing().when(dispatcher).publish(eq(dr), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/detection-report", NODE_ID))
                .andExpect(status().isNoContent());
        verify(dispatcher).publish(eq(dr), eq(NODE_ID), any());
    }

    @Test
    void sendDetectionReport_returns404_whenNodeNotFound() throws Exception {
        when(registry.getNode(NODE_ID)).thenReturn(Optional.empty());

        mvc.perform(post("/nodes/{id}/detection-report", NODE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void sendDetectionReport_returns404_whenNoDetectionReportJson() throws Exception {
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));

        mvc.perform(post("/nodes/{id}/detection-report", NODE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void sendDetectionReport_reloadsFixtures_whenAutoReloadEnabled() throws Exception {
        DetectionReport dr =
                DetectionReport.newBuilder().setReportId("dr-1").setObjectId("obj-1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        null,
                        dr,
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        when(registry.isAutoReloadOnManualSend()).thenReturn(true);
        doNothing().when(dispatcher).publish(eq(dr), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/detection-report", NODE_ID))
                .andExpect(status().isNoContent());
        verify(registry).reload();
    }

    @Test
    void sendDetectionReport_doesNotReload_whenAutoReloadDisabled() throws Exception {
        DetectionReport dr =
                DetectionReport.newBuilder().setReportId("dr-1").setObjectId("obj-1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        null,
                        dr,
                        true);
        when(registry.getNode(NODE_ID)).thenReturn(Optional.of(node));
        doNothing().when(dispatcher).publish(eq(dr), eq(NODE_ID), any());

        mvc.perform(post("/nodes/{id}/detection-report", NODE_ID))
                .andExpect(status().isNoContent());
        verify(registry, never()).reload();
    }

    @Test
    void list_reflectsHasDetectionReport() throws Exception {
        DetectionReport dr = DetectionReport.newBuilder().setObjectId("obj-1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        null,
                        dr,
                        true);
        when(registry.getNodes()).thenReturn(List.of(node));

        mvc.perform(get("/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hasDetectionReport").value(true));
    }

    @Test
    void list_reflectsHasAlert() throws Exception {
        Alert alert = Alert.newBuilder().setAlertId("a1").build();
        EdgeNode node =
                new EdgeNode(
                        NODE_ID,
                        Registration.getDefaultInstance(),
                        StatusReport.getDefaultInstance(),
                        alert,
                        true);
        when(registry.getNodes()).thenReturn(List.of(node));

        mvc.perform(get("/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hasAlert").value(true));
    }
}
