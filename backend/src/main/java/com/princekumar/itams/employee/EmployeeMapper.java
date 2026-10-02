package com.princekumar.itams.employee;

import com.princekumar.itams.employee.dto.EmployeeResponse;

final class EmployeeMapper {
    private EmployeeMapper() {}
    static EmployeeResponse toResponse(Employee e) {
        var p = e.getPerson();
        var d = e.getDepartment();
        return new EmployeeResponse(
            e.getId(),
            p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(),
            e.getEmployeeNumber(),
            d.getId(), d.getCode(),
            e.getJobTitle(),
            e.getHireDate(), e.getEndDate(),
            e.getEmploymentStatus(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
