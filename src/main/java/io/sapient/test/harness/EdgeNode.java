package io.sapient.test.harness;

import io.sapient.transmission.INode;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import uk.gov.dstl.sapientmsg.bsiflex335v2.AlertAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Registration;
import uk.gov.dstl.sapientmsg.bsiflex335v2.RegistrationAck;
import uk.gov.dstl.sapientmsg.bsiflex335v2.StatusReport;
import uk.gov.dstl.sapientmsg.bsiflex335v2.Task;

class EdgeNode implements INode {

    @Getter private final UUID nodeId;

    private final AtomicReference<Registration> registration;
    private final AtomicReference<StatusReport> statusReport;
    private final AtomicBoolean online;

    EdgeNode(UUID nodeId, Registration registration, StatusReport statusReport, boolean online) {
        this.nodeId = nodeId;
        this.registration = new AtomicReference<>(registration);
        this.statusReport = new AtomicReference<>(statusReport);
        this.online = new AtomicBoolean(online);
    }

    public Registration getRegistration() {
        return registration.get();
    }

    void setRegistration(Registration registration) {
        this.registration.set(registration);
    }

    public StatusReport getStatusReport() {
        return statusReport.get();
    }

    void setStatusReport(StatusReport statusReport) {
        this.statusReport.set(statusReport);
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
}