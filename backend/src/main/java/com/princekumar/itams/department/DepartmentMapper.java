package com.princekumar.itams.department;

import com.princekumar.itams.department.dto.DepartmentResponse;

/**
 * Small, hand-written mapper (JPA entity ↔ response DTO).
 *
 * <p>We keep this hand-written rather than pulling in MapStruct — a
 * single mapping is not worth the annotation-processor overhead. When
 * the number of mappers grows past ~5 we'll revisit that decision.</p>
 */
final class DepartmentMapper {

    private DepartmentMapper() { /* utility */ }

    static DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
            d.getId(),
            d.getCode(),
            d.getName(),
            d.getParentDepartment() != null ? d.getParentDepartment().getId() : null,
            d.getManagerPersonId(),
            d.getCreatedAt(),
            d.getUpdatedAt()
        );
    }
}
