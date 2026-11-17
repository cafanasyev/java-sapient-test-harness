package io.sapient.test.harness;

import io.sapient.transmission.INode;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Alert;
import uk.gov.dstl.sapientmsg.bsiflex335v2.AlertAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.DetectionReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Error;
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

    @Override
    public Registration getRegistration() {
        return registration.get();
    }

    void setRegistration(Registration registration) {
        this.registration.set(registration);
    }

    /** A missing {@code reportId} stays missing: the SDK gives every report a new one. */
    @Override
    public StatusReport getStatusReport() {
        return statusReport.get();
    }

    void setStatusReport(StatusReport statusReport) {
        this.statusReport.set(statusReport);
    }

    /** A missing {@code alertId} stays missing: the SDK gives every alert a new one. */
    public Optional<Alert> getAlert() {
        return Optional.ofNullable(alert.get());
    }

    void setAlert(Alert alert) {
        this.alert.set(alert);
    }

    /** A missing {@code reportId} stays missing: the SDK gives every report a new one. */
    public Optional<DetectionReport> getDetectionReport() {
        return Optional.ofNullable(detectionReport.get());
    }

    void setDetectionReport(DetectionReport detectionReport) {
        this.detectionReport.set(detectionReport);
    }

    public boolean hasRegistration() {
        return registration.get() != null;
    }

    public String getLabel() {
        Registration r = registration.get();
        if (r == null) return "";
        if (r.hasShortName() && !r.getShortName().isEmpty()) return r.getShortName();
        if (r.hasName() && !r.getName().isEmpty()) return r.getName();
        return "";
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

    @Override
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

    @Override
    public void onError(Error error) {}
}
