package io.sapient.test.harness;

import com.google.protobuf.util.JsonFormat;
import io.sapient.transmission.INode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.DetectionReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;

/**
 * Loads {@link INode} instances from an {@code edge_nodes} directory.
 *
 * <p>Expected layout:
 *
 * <pre>
 * edge_nodes/
 *   {uuid}/
 *     registration.json
 *     status_report.json
 * </pre>
 */
@Component
public class EdgeNodeLoader {

    private static final String EDGE_NODES_DIR = "edge_nodes";

    /**
     * Scans {@code baseDir/edge_nodes/} and returns one {@link INode} per UUID subfolder.
     *
     * @throws IllegalArgumentException if any subdirectory name is not a valid UUID
     */
    public List<INode> load(Path baseDir) throws IOException {
        Path edgeNodesDir = baseDir.resolve(EDGE_NODES_DIR);
        if (!Files.isDirectory(edgeNodesDir)) {
            return List.of();
        }

        List<INode> nodes = new ArrayList<>();
        try (Stream<Path> entries = Files.list(edgeNodesDir)) {
            for (Path entry : (Iterable<Path>) entries::iterator) {
                if (!Files.isDirectory(entry)) continue;
                nodes.add(loadNode(UUID.fromString(entry.getFileName().toString()), entry));
            }
        }
        return nodes;
    }

    private INode loadNode(UUID nodeId, Path nodeDir) throws IOException {
        Registration registration = parseRegistration(nodeDir.resolve("registration.json"));
        StatusReport statusReport = parseStatusReport(nodeDir.resolve("status_report.json"));
        Path alertFile = nodeDir.resolve("alert.json");
        Alert alert = Files.exists(alertFile) ? parseAlert(alertFile) : null;
        Path detectionReportFile = nodeDir.resolve("detection_report.json");
        DetectionReport detectionReport =
                Files.exists(detectionReportFile)
                        ? parseDetectionReport(detectionReportFile)
                        : null;
        return new EdgeNode(nodeId, registration, statusReport, alert, detectionReport, false);
    }

    private Registration parseRegistration(Path file) throws IOException {
        Registration.Builder builder = Registration.newBuilder();
        JsonFormat.parser().merge(Files.readString(file), builder);
        return builder.build();
    }

    private StatusReport parseStatusReport(Path file) throws IOException {
        StatusReport.Builder builder = StatusReport.newBuilder();
        JsonFormat.parser().merge(Files.readString(file), builder);
        return builder.build();
    }

    private Alert parseAlert(Path file) throws IOException {
        Alert.Builder builder = Alert.newBuilder();
        JsonFormat.parser().merge(Files.readString(file), builder);
        return builder.build();
    }

    private DetectionReport parseDetectionReport(Path file) throws IOException {
        DetectionReport.Builder builder = DetectionReport.newBuilder();
        JsonFormat.parser().merge(Files.readString(file), builder);
        return builder.build();
    }
}
