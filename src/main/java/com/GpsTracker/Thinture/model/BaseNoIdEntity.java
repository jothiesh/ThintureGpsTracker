package com.GpsTracker.Thinture.model;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseNoIdEntity {
    // Only shared fields, no @Id
    public abstract Long getAdmin_id();
    public abstract void setAdmin_id(Long admin_id);

    // etc.
}
