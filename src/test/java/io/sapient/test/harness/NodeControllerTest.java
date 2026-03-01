package io.sapient.test.harness;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

@Execution(ExecutionMode.CONCURRENT)
class NodeControllerTest {

    private EdgeNodeRegistry registry;
    private MockMvc mvc;

    private static final UUID NODE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        registry = mock(EdgeNodeRegistry.class);
        mvc =
                MockMvcBuilders.standaloneSetup(new NodeController(registry, new ObjectMapper()))
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

        mvc.perform(
                        put("/nodes/{id}/online", NODE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("false"))
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

        mvc.perform(
                        put("/nodes/{id}/online", NODE_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("true"))
                .andExpect(status().isNotFound());
    }
}
