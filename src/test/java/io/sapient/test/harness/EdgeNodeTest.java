package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.sapient.transmission.INodeDispatcher;
import io.sapient.transmission.NodeDispatcher;
import io.sapient.transmission.NodeDispatcherConfig;
import io.sapient.transport.IClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.AlertAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.DetectionReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.RegistrationAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.SapientMessage;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Task;

@Execution(ExecutionMode.CONCURRENT)
class EdgeNodeTest {

    private static final UUID NODE_ID = UUID.randomUUID();
    private static final Duration TIMEOUT = Duration.ofSeconds(1);
    private static final Registration REGISTRATION =
            Registration.newBuilder().setIcdVersion("BSI Flex 335 v2.0").build();
    private static final StatusReport STATUS_REPORT =
            StatusReport.newBuilder()
                    .setSystem(StatusReport.System.SYSTEM_OK)
                    .setReportId("test-report-id")
                    .build();

    private final EdgeNode node = new EdgeNode(NODE_ID, REGISTRATION, STATUS_REPORT, true);

    @Test
    void isOnline_returnsTrue() {
        assertTrue(node.isOnline());
    }

    @Test
    void getNodeId_returnsConstructedId() {
        assertEquals(NODE_ID, node.getNodeId());
    }

    @Test
    void getRegistration_returnsConstructedRegistration() {
        assertEquals(REGISTRATION, node.getRegistration());
    }

    @Test
    void getStatusReport_returnsConstructedStatusReport() {
        assertEquals(STATUS_REPORT, node.getStatusReport());
    }

    @Test
    void statusReport_getsNewReportIdOnEverySend() throws Exception {
        EdgeNode fresh =
                new EdgeNode(NODE_ID, REGISTRATION, StatusReport.getDefaultInstance(), false);
        List<SapientMessage> sent =
                sendTwice(fresh, d -> d.publish(fresh.getStatusReport(), NODE_ID, TIMEOUT));
        assertNotEquals(
                sent.get(0).getStatusReport().getReportId(),
                sent.get(1).getStatusReport().getReportId());
    }

    @Test
    void setRegistration_updatesReturnedValue() {
        Registration updated = Registration.newBuilder().setIcdVersion("updated").build();
        node.setRegistration(updated);
        assertEquals(updated, node.getRegistration());
    }

    @Test
    void setStatusReport_updatesReturnedValue() {
        StatusReport updated =
                StatusReport.newBuilder()
                        .setSystem(StatusReport.System.SYSTEM_WARNING)
                        .setReportId("updated-id")
                        .build();
        node.setStatusReport(updated);
        assertEquals(updated, node.getStatusReport());
    }

    @Test
    void setOnline_updatesReturnedValue() {
        node.setOnline(false);
        assertFalse(node.isOnline());
        node.setOnline(true);
        assertTrue(node.isOnline());
    }

    @Test
    void getAlert_returnsEmpty_whenNotSet() {
        assertTrue(node.getAlert().isEmpty());
    }

    @Test
    void hasAlert_returnsFalse_whenNotSet() {
        assertFalse(node.hasAlert());
    }

    @Test
    void setAlert_updatesReturnedValue() {
        Alert alert = Alert.newBuilder().setAlertId("a1").build();
        node.setAlert(alert);
        assertEquals(alert, node.getAlert().orElseThrow());
        assertTrue(node.hasAlert());
    }

    @Test
    void alert_getsNewAlertIdOnEverySend() throws Exception {
        node.setAlert(Alert.newBuilder().setAlertType(Alert.AlertType.ALERT_TYPE_WARNING).build());
        List<SapientMessage> sent =
                sendTwice(node, d -> d.publish(node.getAlert().orElseThrow(), NODE_ID, TIMEOUT));
        assertNotEquals(sent.get(0).getAlert().getAlertId(), sent.get(1).getAlert().getAlertId());
    }

    @Test
    void setAlert_null_clearsAlert() {
        node.setAlert(Alert.newBuilder().setAlertId("a1").build());
        node.setAlert(null);
        assertTrue(node.getAlert().isEmpty());
        assertFalse(node.hasAlert());
    }

    @Test
    void getDetectionReport_returnsEmpty_whenNotSet() {
        assertTrue(node.getDetectionReport().isEmpty());
    }

    @Test
    void hasDetectionReport_returnsFalse_whenNotSet() {
        assertFalse(node.hasDetectionReport());
    }

    @Test
    void setDetectionReport_updatesReturnedValue() {
        DetectionReport dr =
                DetectionReport.newBuilder().setReportId("r1").setObjectId("obj-1").build();
        node.setDetectionReport(dr);
        assertEquals(dr, node.getDetectionReport().orElseThrow());
        assertTrue(node.hasDetectionReport());
    }

    @Test
    void detectionReport_getsNewReportIdOnEverySend() throws Exception {
        node.setDetectionReport(DetectionReport.newBuilder().setObjectId("obj-1").build());
        List<SapientMessage> sent =
                sendTwice(
                        node,
                        d -> d.publish(node.getDetectionReport().orElseThrow(), NODE_ID, TIMEOUT));
        assertNotEquals(
                sent.get(0).getDetectionReport().getReportId(),
                sent.get(1).getDetectionReport().getReportId());
    }

    @Test
    void setDetectionReport_null_clearsDetectionReport() {
        node.setDetectionReport(DetectionReport.newBuilder().setObjectId("obj-1").build());
        node.setDetectionReport(null);
        assertTrue(node.getDetectionReport().isEmpty());
        assertFalse(node.hasDetectionReport());
    }

    @Test
    void getLabel_returnsShortName_whenSet() {
        Registration r =
                Registration.newBuilder().setName("Full Name").setShortName("Short").build();
        EdgeNode n = new EdgeNode(NODE_ID, r, STATUS_REPORT, true);
        assertEquals("Short", n.getLabel());
    }

    @Test
    void getLabel_fallsBackToName_whenShortNameMissing() {
        Registration r = Registration.newBuilder().setName("Full Name").build();
        EdgeNode n = new EdgeNode(NODE_ID, r, STATUS_REPORT, true);
        assertEquals("Full Name", n.getLabel());
    }

    @Test
    void getLabel_fallsBackToName_whenShortNameEmpty() {
        Registration r = Registration.newBuilder().setName("Full Name").setShortName("").build();
        EdgeNode n = new EdgeNode(NODE_ID, r, STATUS_REPORT, true);
        assertEquals("Full Name", n.getLabel());
    }

    @Test
    void getLabel_returnsEmpty_whenBothMissing() {
        EdgeNode n = new EdgeNode(NODE_ID, Registration.getDefaultInstance(), STATUS_REPORT, true);
        assertEquals("", n.getLabel());
    }

    @Test
    void getLabel_returnsEmpty_whenNameEmpty() {
        Registration r = Registration.newBuilder().setName("").build();
        EdgeNode n = new EdgeNode(NODE_ID, r, STATUS_REPORT, true);
        assertEquals("", n.getLabel());
    }

    @Test
    void getLabel_returnsEmpty_whenRegistrationNull() {
        EdgeNode n = new EdgeNode(NODE_ID, null, STATUS_REPORT, true);
        assertEquals("", n.getLabel());
    }

    @Test
    void onRegistrationAck_doesNotThrow() {
        assertDoesNotThrow(() -> node.onRegistrationAck(RegistrationAck.getDefaultInstance()));
    }

    @Test
    void onAlertAck_doesNotThrow() {
        assertDoesNotThrow(() -> node.onAlertAck(AlertAck.getDefaultInstance()));
    }

    @Test
    void onTask_doesNotThrow() {
        assertDoesNotThrow(() -> node.onTask(Task.getDefaultInstance()));
    }

    @FunctionalInterface
    private interface Send {
        void to(INodeDispatcher dispatcher) throws Exception;
    }

    /** Sends twice through a real dispatcher and returns the two messages the client got. */
    private static List<SapientMessage> sendTwice(EdgeNode node, Send send) throws Exception {
        IClient client = mock(IClient.class);
        try (NodeDispatcher dispatcher =
                new NodeDispatcher(client, NodeDispatcherConfig.defaults(UUID.randomUUID()))) {
            dispatcher.register(node);
            send.to(dispatcher);
            send.to(dispatcher);
            ArgumentCaptor<SapientMessage> sent = ArgumentCaptor.forClass(SapientMessage.class);
            verify(client, times(2)).publish(sent.capture(), any());
            return sent.getAllValues();
        }
    }
}
