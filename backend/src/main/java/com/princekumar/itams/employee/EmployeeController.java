package com.princekumar.itams.employee;

import com.princekumar.itams.common.dto.PageResponse;
import com.princekumar.itams.employee.dto.EmployeeCreateRequest;
import com.princekumar.itams.employee.dto.EmployeeResponse;
import com.princekumar.itams.employee.dto.EmployeeUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Employment records attached to people")
public class EmployeeController {

    private final EmployeeService service;
    public EmployeeController(EmployeeService service) { this.service = service; }

    @Operation(summary = "Create an employee record for an existing person")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest req, UriComponentsBuilder uri) {
        Employee e = service.create(req);
        URI location = uri.path("/api/v1/employees/{id}").buildAndExpand(e.getId()).toUri();
        return ResponseEntity.created(location).body(EmployeeMapper.toResponse(e));
    }

    @Operation(summary = "Get an employee by id")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return EmployeeMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "List employees (paginated, optional search + department filter)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @GetMapping
    public PageResponse<EmployeeResponse> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Long departmentId,
                                               Pageable pageable) {
        return PageResponse.from(service.search(q, departmentId, pageable), EmployeeMapper::toResponse);
    }

    @Operation(summary = "Update an employee (partial)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest req) {
        return EmployeeMapper.toResponse(service.update(id, req));
    }

    @Operation(summary = "Delete an employee record")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
