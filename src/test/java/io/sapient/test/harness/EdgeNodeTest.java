package io.sapient.test.harness;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.gov.dstl.sapientmsg.bsiflex335v2.AlertAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.RegistrationAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Task;

@Execution(ExecutionMode.CONCURRENT)
class EdgeNodeTest {

    private static final UUID NODE_ID = UUID.randomUUID();
    private static final Registration REGISTRATION =
            Registration.newBuilder().setIcdVersion("BSI Flex 335 v2.0").build();
    private static final StatusReport STATUS_REPORT =
            StatusReport.newBuilder().setSystem(StatusReport.System.SYSTEM_OK).build();

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
    void setRegistration_updatesReturnedValue() {
        Registration updated = Registration.newBuilder().setIcdVersion("updated").build();
        node.setRegistration(updated);
        assertEquals(updated, node.getRegistration());
    }

    @Test
    void setStatusReport_updatesReturnedValue() {
        StatusReport updated =
                StatusReport.newBuilder().setSystem(StatusReport.System.SYSTEM_WARNING).build();
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
}
