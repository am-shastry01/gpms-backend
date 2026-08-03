package com.gpms.backend.gatepass.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "gate_pass_items")
public class GatePassItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gate_pass_request_id", nullable = false)
    private GatePassRequest gatePassRequest;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    public GatePassRequest getGatePassRequest() {
        return gatePassRequest;
    }

    public void setGatePassRequest(GatePassRequest gatePassRequest) {
        this.gatePassRequest = gatePassRequest;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
