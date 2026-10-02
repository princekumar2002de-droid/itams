package com.princekumar.itams.license;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A commercial software product, e.g. "Microsoft 365 Business Standard".
 * Products are created on demand when a licence is created; there is no separate CRUD API.
 */
@Entity
@Table(name = "software_product",
       uniqueConstraints = @UniqueConstraint(name = "ux_software_product", columnNames = {"vendor", "name", "version"}))
public class SoftwareProduct extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String vendor;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 60)
    private String version;

    protected SoftwareProduct() { /* JPA */ }

    public SoftwareProduct(String vendor, String name, String version) {
        this.vendor = vendor;
        this.name = name;
        this.version = version;
    }

    public String getVendor() { return vendor; }
    public String getName() { return name; }
    public String getVersion() { return version; }
}
