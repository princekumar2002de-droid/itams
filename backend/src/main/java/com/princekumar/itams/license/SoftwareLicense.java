package com.princekumar.itams.license;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A purchased entitlement — N seats of a specific product, purchased on
 * a specific date, with an optional expiry.
 */
@Entity
@Table(name = "software_license",
       uniqueConstraints = @UniqueConstraint(name = "ux_software_license",
                                             columnNames = {"product_id", "license_reference"}))
public class SoftwareLicense extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private SoftwareProduct product;

    @Column(name = "license_reference", nullable = false, length = 120, updatable = false)
    private String licenseReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_type", nullable = false, length = 20, updatable = false)
    private LicenseType licenseType;

    @Column(name = "seats_total", nullable = false)
    private int seatsTotal;

    @Column(name = "purchase_date", nullable = false, updatable = false)
    private LocalDate purchaseDate;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "procurement_ref", length = 120)
    private String procurementRef;

    protected SoftwareLicense() { /* JPA */ }

    public SoftwareLicense(SoftwareProduct product, String licenseReference, LicenseType licenseType,
                           int seatsTotal, LocalDate purchaseDate, LocalDate expiresOn,
                           BigDecimal cost, String procurementRef) {
        this.product = product;
        this.licenseReference = licenseReference;
        this.licenseType = licenseType;
        this.seatsTotal = seatsTotal;
        this.purchaseDate = purchaseDate;
        this.expiresOn = expiresOn;
        this.cost = cost;
        this.procurementRef = procurementRef;
    }

    public SoftwareProduct getProduct() { return product; }
    public String getLicenseReference() { return licenseReference; }
    public LicenseType getLicenseType() { return licenseType; }
    public int getSeatsTotal() { return seatsTotal; }
    public void setSeatsTotal(int seatsTotal) { this.seatsTotal = seatsTotal; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public BigDecimal getCost() { return cost; }
    public String getProcurementRef() { return procurementRef; }
}
