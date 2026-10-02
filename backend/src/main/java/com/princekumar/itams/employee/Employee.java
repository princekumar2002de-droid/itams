package com.princekumar.itams.employee;

import com.princekumar.itams.common.entity.BaseEntity;
import com.princekumar.itams.department.Department;
import com.princekumar.itams.person.Person;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * The employment record attached to a {@link Person} (1:1). One person may
 * or may not have an Employee row — external contractors are Person only.
 */
@Entity
@Table(name = "employee")
public class Employee extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false, unique = true, updatable = false)
    private Person person;

    @Column(name = "employee_number", nullable = false, length = 30, unique = true)
    private String employeeNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 20)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    protected Employee() { /* JPA */ }

    public Employee(Person person, String employeeNumber, Department department,
                    String jobTitle, LocalDate hireDate) {
        this.person = person;
        this.employeeNumber = employeeNumber;
        this.department = department;
        this.jobTitle = jobTitle;
        this.hireDate = hireDate;
    }

    public Person getPerson() { return person; }
    public String getEmployeeNumber() { return employeeNumber; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department d) { this.department = d; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String v) { this.jobTitle = v; }
    public LocalDate getHireDate() { return hireDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(EmploymentStatus v) { this.employmentStatus = v; }
}
