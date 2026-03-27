package io.sapient.test.harness;

import com.github.f4b6a3.ulid.UlidCreator;
import io.sapient.transmission.INode;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.AlertAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.DetectionReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.RegistrationAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Task;

class EdgeNode implements INode {

    @Getter private final UUID nodeId;

    private final AtomicReference<Registration> registration;
    private final AtomicReference<StatusReport> statusReport;
    private final AtomicReference<Alert> alert;
    private final AtomicReference<DetectionReport> detectionReport;
    private final AtomicBoolean online;

    EdgeNode(UUID nodeId, Registration registration, StatusReport statusReport, boolean online) {
        this(nodeId, registration, statusReport, null, null, online);
    }

    EdgeNode(
            UUID nodeId,
            Registration registration,
            StatusReport statusReport,
            Alert alert,
            boolean online) {
        this(nodeId, registration, statusReport, alert, null, online);
    }

    EdgeNode(
            UUID nodeId,
            Registration registration,
            StatusReport statusReport,
            Alert alert,
            DetectionReport detectionReport,
            boolean online) {
        this.nodeId = nodeId;
        this.registration = new AtomicReference<>(registration);
        this.statusReport = new AtomicReference<>(statusReport);
        this.alert = new AtomicReference<>(alert);
        this.detectionReport = new AtomicReference<>(detectionReport);
        this.online = new AtomicBoolean(online);
    }

    public Registration getRegistration() {
        return registration.get();
    }

    void setRegistration(Registration registration) {
        this.registration.set(registration);
    }

    public StatusReport getStatusReport() {
        return statusReport.updateAndGet(EdgeNode::withReportId);
    }

    void setStatusReport(StatusReport statusReport) {
        this.statusReport.set(statusReport);
    }

    public Optional<Alert> getAlert() {
        return Optional.ofNullable(alert.updateAndGet(EdgeNode::withAlertId));
    }

    void setAlert(Alert alert) {
        this.alert.set(alert);
    }

    public Optional<DetectionReport> getDetectionReport() {
        return Optional.ofNullable(detectionReport.updateAndGet(EdgeNode::withDetectionReportId));
    }

    void setDetectionReport(DetectionReport detectionReport) {
        this.detectionReport.set(detectionReport);
    }

    public boolean hasRegistration() {
        return registration.get() != null;
    }

    public boolean hasStatusReport() {
        return statusReport.get() != null;
    }

    public boolean hasAlert() {
        return alert.get() != null;
    }

    public boolean hasDetectionReport() {
        return detectionReport.get() != null;
    }

    public boolean isOnline() {
        return online.get();
    }

    void setOnline(boolean online) {
        this.online.set(online);
    }

    @Override
    public void onRegistrationAck(RegistrationAck ack) {}

    @Override
    public void onAlertAck(AlertAck ack) {}

    @Override
    public void onTask(Task task) {}

    private static StatusReport withReportId(StatusReport sr) {
        if (sr.hasReportId()) return sr;
        return sr.toBuilder().setReportId(UlidCreator.getMonotonicUlid().toString()).build();
    }

    private static Alert withAlertId(Alert a) {
        if (a == null || a.hasAlertId()) return a;
        return a.toBuilder().setAlertId(UlidCreator.getMonotonicUlid().toString()).build();
    }

    private static DetectionReport withDetectionReportId(DetectionReport dr) {
        if (dr == null || dr.hasReportId()) return dr;
        return dr.toBuilder().setReportId(UlidCreator.getMonotonicUlid().toString()).build();
    }
}
