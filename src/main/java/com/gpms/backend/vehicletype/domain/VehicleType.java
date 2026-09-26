package com.gpms.backend.vehicletype.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A class of vehicle - Truck, Tempo, Container and so on.
 *
 * Only active types are offered in the request form, so a type
 * that is retired stops being selectable without breaking the
 * gate passes that already reference it.
 */
@Entity
@Table(name = "vehicle_types")
public class VehicleType extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
