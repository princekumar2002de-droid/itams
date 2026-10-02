package com.princekumar.itams.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Loads the associations the API response reads ("person", "department") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"person", "department"})
    @Override
    Optional<Employee> findById(Long id);

    boolean existsByEmployeeNumber(String employeeNumber);

    @EntityGraph(attributePaths = {"person", "department"})
    Optional<Employee> findByPersonId(Long personId);

    @Query("""
        SELECT e FROM Employee e
         WHERE (:q IS NULL OR :q = ''
                OR LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.person.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(e.person.lastName)  LIKE LOWER(CONCAT('%', :q, '%')))
           AND (:departmentId IS NULL OR e.department.id = :departmentId)
    """)
    @EntityGraph(attributePaths = {"person", "department"})
    Page<Employee> search(String q, Long departmentId, Pageable pageable);
}
