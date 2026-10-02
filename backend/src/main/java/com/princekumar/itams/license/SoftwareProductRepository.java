package com.princekumar.itams.license;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SoftwareProductRepository extends JpaRepository<SoftwareProduct, Long> {

    Optional<SoftwareProduct> findByVendorIgnoreCaseAndNameIgnoreCaseAndVersionIsNull(String vendor, String name);

    Optional<SoftwareProduct> findByVendorIgnoreCaseAndNameIgnoreCaseAndVersionIgnoreCase(
        String vendor, String name, String version);

    /**
     * Natural key = (vendor, name, version), case-insensitive; version may be null.
     *
     * <p>Two derived queries instead of one JPQL with {@code (:version IS NULL AND ...)}:
     * when {@code version} was null, the PostgreSQL JDBC driver could not infer the
     * parameter type, sent it as {@code bytea}, and {@code LOWER(bytea)} failed — so
     * creating a license for an unversioned product returned 500.</p>
     */
    default Optional<SoftwareProduct> findByNaturalKey(String vendor, String name, String version) {
        return version == null
            ? findByVendorIgnoreCaseAndNameIgnoreCaseAndVersionIsNull(vendor, name)
            : findByVendorIgnoreCaseAndNameIgnoreCaseAndVersionIgnoreCase(vendor, name, version);
    }
}
