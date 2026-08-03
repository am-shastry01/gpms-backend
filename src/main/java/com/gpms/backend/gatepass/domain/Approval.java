package com.gpms.backend.gatepass.domain;

import com.gpms.backend.common.domain.BaseEntity;
import com.gpms.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "approvals")
public class Approval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gate_pass_request_id", nullable = false)
    private GatePassRequest gatePassRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_user_id", nullable = false)
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ApprovalAction action;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "action_time", nullable = false)
    private Instant actionTime;

    public GatePassRequest getGatePassRequest() {
        return gatePassRequest;
    }

    public void setGatePassRequest(GatePassRequest gatePassRequest) {
        this.gatePassRequest = gatePassRequest;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public void setAction(ApprovalAction action) {
        this.action = action;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Instant getActionTime() {
        return actionTime;
    }

    public void setActionTime(Instant actionTime) {
        this.actionTime = actionTime;
    }
}
