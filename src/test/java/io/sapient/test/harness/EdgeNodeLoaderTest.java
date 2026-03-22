package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.sapient.transmission.INode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

@Execution(ExecutionMode.CONCURRENT)
class EdgeNodeLoaderTest {

    private static final UUID FIXTURE_NODE_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String FIXTURE_REGISTRATION =
            "edge_nodes/" + FIXTURE_NODE_ID + "/registration.json";
    private static final String FIXTURE_STATUS_REPORT =
            "edge_nodes/" + FIXTURE_NODE_ID + "/status_report.json";
    private static final String FIXTURE_ALERT = "edge_nodes/" + FIXTURE_NODE_ID + "/alert.json";

    private final EdgeNodeLoader loader = new EdgeNodeLoader();

    @TempDir Path tempDir;

    @Test
    void load_returnsEmptyList_whenEdgeNodesDirAbsent() throws IOException {
        assertTrue(loader.load(tempDir).isEmpty());
    }

    @Test
    void load_returnsEmptyList_whenEdgeNodesDirIsEmpty() throws IOException {
        Files.createDirectory(tempDir.resolve("edge_nodes"));
        assertTrue(loader.load(tempDir).isEmpty());
    }

    @Test
    void load_throwsIllegalArgumentException_forNonUuidFolder() throws IOException {
        Path edgeNodes = Files.createDirectory(tempDir.resolve("edge_nodes"));
        Files.createDirectory(edgeNodes.resolve("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> loader.load(tempDir));
    }

    @Test
    void load_skipsFilesInEdgeNodesDir() throws IOException {
        Path edgeNodes = Files.createDirectory(tempDir.resolve("edge_nodes"));
        Files.writeString(edgeNodes.resolve(UUID.randomUUID().toString()), "content");
        assertTrue(loader.load(tempDir).isEmpty());
    }

    @Test
    void load_returnsNode_forValidUuidFolder() throws IOException, URISyntaxException {
        List<INode> nodes = loader.load(resourcesRoot());
        assertEquals(1, nodes.size());
        assertEquals(FIXTURE_NODE_ID, nodes.getFirst().getNodeId());
    }

    @Test
    void load_returnsNodesAsOffline() throws IOException, URISyntaxException {
        EdgeNode node = (EdgeNode) loader.load(resourcesRoot()).getFirst();
        assertFalse(node.isOnline());
    }

    @Test
    void load_parsesRegistrationFromJson() throws IOException, URISyntaxException {
        Registration reg = loader.load(resourcesRoot()).getFirst().getRegistration();
        assertEquals("BSI Flex 335 v2.0", reg.getIcdVersion());
        assertEquals(Registration.NodeType.NODE_TYPE_RADAR, reg.getNodeDefinition(0).getNodeType());
    }

    @Test
    void load_parsesStatusReportFromJson() throws IOException, URISyntaxException {
        StatusReport sr = loader.load(resourcesRoot()).getFirst().getStatusReport();
        assertEquals(StatusReport.System.SYSTEM_OK, sr.getSystem());
        assertEquals(StatusReport.Info.INFO_NEW, sr.getInfo());
        assertEquals("default", sr.getMode());
    }

    @Test
    void load_parsesAlertFromJson_whenPresent() throws IOException, URISyntaxException {
        EdgeNode node = (EdgeNode) loader.load(resourcesRoot()).getFirst();
        assertTrue(node.getAlert().isPresent());
        Alert alert = node.getAlert().get();
        assertEquals("01JPCXQ3FGHS7KBVNE2M4D5R8W", alert.getAlertId());
        assertEquals(Alert.AlertType.ALERT_TYPE_WARNING, alert.getAlertType());
    }

    @Test
    void load_returnsEmptyAlert_whenAlertJsonAbsent() throws IOException {
        Path nodeDir =
                Files.createDirectories(
                        tempDir.resolve("edge_nodes").resolve(UUID.randomUUID().toString()));
        copyResource(FIXTURE_REGISTRATION, nodeDir.resolve("registration.json"));
        copyResource(FIXTURE_STATUS_REPORT, nodeDir.resolve("status_report.json"));
        EdgeNode node = (EdgeNode) loader.load(tempDir).getFirst();
        assertFalse(node.getAlert().isPresent());
    }

    @Test
    void load_returnsMultipleNodes_forMultipleUuidFolders() throws IOException {
        createNodeDir(tempDir);
        createNodeDir(tempDir);
        assertEquals(2, loader.load(tempDir).size());
    }

    @Test
    void load_throwsIOException_whenRegistrationJsonMissing() throws IOException {
        Path nodeDir =
                Files.createDirectories(
                        tempDir.resolve("edge_nodes").resolve(UUID.randomUUID().toString()));
        copyResource(FIXTURE_STATUS_REPORT, nodeDir.resolve("status_report.json"));
        assertThrows(IOException.class, () -> loader.load(tempDir));
    }

    @Test
    void load_throwsIOException_whenStatusReportJsonMissing() throws IOException {
        Path nodeDir =
                Files.createDirectories(
                        tempDir.resolve("edge_nodes").resolve(UUID.randomUUID().toString()));
        copyResource(FIXTURE_REGISTRATION, nodeDir.resolve("registration.json"));
        assertThrows(IOException.class, () -> loader.load(tempDir));
    }

    private Path resourcesRoot() throws URISyntaxException {
        return Path.of(getClass().getClassLoader().getResource("edge_nodes").toURI()).getParent();
    }

    private UUID createNodeDir(Path baseDir) throws IOException {
        UUID nodeId = UUID.randomUUID();
        Path edgeNodes = baseDir.resolve("edge_nodes");
        if (!Files.exists(edgeNodes)) {
            Files.createDirectory(edgeNodes);
        }
        Path nodeDir = Files.createDirectory(edgeNodes.resolve(nodeId.toString()));
        copyResource(FIXTURE_REGISTRATION, nodeDir.resolve("registration.json"));
        copyResource(FIXTURE_STATUS_REPORT, nodeDir.resolve("status_report.json"));
        return nodeId;
    }

    private void copyResource(String resource, Path target) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resource);
        if (in == null) throw new IOException("Test resource not found: " + resource);
        try (in) {
            Files.copy(in, target);
        }
    }
}
