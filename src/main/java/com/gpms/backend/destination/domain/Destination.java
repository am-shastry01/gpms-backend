package com.gpms.backend.destination.domain;

import com.gpms.backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A place a consignment can be dispatched to.
 *
 * Destination used to be free text typed into every request, which
 * meant "Pune", "pune" and "Pune " were three different places as
 * far as any report was concerned. It is a managed list now.
 */
@Entity
@Table(name = "destinations")
public class Destination extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 150)
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
