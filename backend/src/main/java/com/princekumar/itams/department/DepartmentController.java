package com.princekumar.itams.department;

import com.princekumar.itams.common.dto.PageResponse;
import com.princekumar.itams.department.dto.DepartmentCreateRequest;
import com.princekumar.itams.department.dto.DepartmentResponse;
import com.princekumar.itams.department.dto.DepartmentUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * REST endpoints for {@code /api/v1/departments}.
 *
 * <p>Deliberately thin: DTO in, DTO out, no business logic. Anything
 * with an "if" belongs in the service.</p>
 */
@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "Organisational departments — CRUD + soft delete")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @Operation(summary = "Create a department")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest req,
                                                     UriComponentsBuilder uri) {
        Department created = service.create(req);
        URI location = uri.path("/api/v1/departments/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(DepartmentMapper.toResponse(created));
    }

    @Operation(summary = "Get a single department by id")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @GetMapping("/{id}")
    public DepartmentResponse getOne(@PathVariable Long id) {
        return DepartmentMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "List departments (paginated, optional search)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @GetMapping
    public PageResponse<DepartmentResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.search(q, pageable), DepartmentMapper::toResponse);
    }

    @Operation(summary = "Update a department (partial)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody DepartmentUpdateRequest req) {
        return DepartmentMapper.toResponse(service.update(id, req));
    }

    @Operation(summary = "Soft-delete a department")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.softDelete(id);
    }
}
