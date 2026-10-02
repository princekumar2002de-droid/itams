package com.princekumar.itams.employee;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.department.Department;
import com.princekumar.itams.department.DepartmentRepository;
import com.princekumar.itams.employee.dto.EmployeeCreateRequest;
import com.princekumar.itams.employee.dto.EmployeeUpdateRequest;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repo;
    private final PersonRepository personRepo;
    private final DepartmentRepository departmentRepo;

    public EmployeeService(EmployeeRepository repo,
                           PersonRepository personRepo,
                           DepartmentRepository departmentRepo) {
        this.repo = repo;
        this.personRepo = personRepo;
        this.departmentRepo = departmentRepo;
    }

    @AuditWrite(entity = "Employee", action = "CREATE")
    public Employee create(EmployeeCreateRequest req) {
        if (repo.existsByEmployeeNumber(req.employeeNumber())) {
            throw new BusinessRuleViolationException(
                "employee.number_already_used",
                "Employee number '%s' is already in use.".formatted(req.employeeNumber()));
        }
        if (repo.findByPersonId(req.personId()).isPresent()) {
            throw new BusinessRuleViolationException(
                "employee.person_already_has_record",
                "Person %d already has an employee record.".formatted(req.personId()));
        }
        Person person = personRepo.findByIdAndDeletedAtIsNull(req.personId())
            .orElseThrow(() -> new ResourceNotFoundException("Person", req.personId()));
        Department dept = departmentRepo.findByIdAndDeletedAtIsNull(req.departmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Department", req.departmentId()));

        Employee e = new Employee(person, req.employeeNumber(), dept, req.jobTitle(), req.hireDate());
        return repo.save(e);
    }

    @AuditWrite(entity = "Employee", action = "UPDATE")
    public Employee update(Long id, EmployeeUpdateRequest req) {
        Employee e = requireById(id);
        if (req.departmentId() != null) {
            Department dept = departmentRepo.findByIdAndDeletedAtIsNull(req.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", req.departmentId()));
            e.setDepartment(dept);
        }
        if (req.jobTitle() != null) e.setJobTitle(req.jobTitle());
        if (req.endDate() != null) {
            if (req.endDate().isBefore(e.getHireDate())) {
                throw new BusinessRuleViolationException(
                    "employee.end_before_hire", "endDate cannot be before hireDate.");
            }
            e.setEndDate(req.endDate());
        }
        if (req.employmentStatus() != null) e.setEmploymentStatus(req.employmentStatus());
        return e;
    }

    public void delete(Long id) { repo.delete(requireById(id)); }

    @Transactional(readOnly = true)
    public Employee findById(Long id) { return requireById(id); }

    @Transactional(readOnly = true)
    public Page<Employee> search(String q, Long departmentId, Pageable pageable) {
        return repo.search(q, departmentId, pageable);
    }

    Employee requireById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }
}
