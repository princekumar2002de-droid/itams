package com.princekumar.itams.person;

import com.princekumar.itams.common.dto.PageResponse;
import com.princekumar.itams.person.dto.PersonCreateRequest;
import com.princekumar.itams.person.dto.PersonResponse;
import com.princekumar.itams.person.dto.PersonUpdateRequest;
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
@RequestMapping("/api/v1/people")
@Tag(name = "People", description = "People (identity behind employees, users and asset assignees)")
public class PersonController {

    private final PersonService service;
    public PersonController(PersonService service) { this.service = service; }

    @Operation(summary = "Create a person")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonCreateRequest req, UriComponentsBuilder uri) {
        Person p = service.create(req);
        URI location = uri.path("/api/v1/people/{id}").buildAndExpand(p.getId()).toUri();
        return ResponseEntity.created(location).body(PersonMapper.toResponse(p));
    }

    @Operation(summary = "Get a person by id")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @GetMapping("/{id}")
    public PersonResponse get(@PathVariable Long id) {
        return PersonMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "Search people (paginated)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @GetMapping
    public PageResponse<PersonResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.search(q, pageable), PersonMapper::toResponse);
    }

    @Operation(summary = "Update a person (partial)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public PersonResponse update(@PathVariable Long id, @Valid @RequestBody PersonUpdateRequest req) {
        return PersonMapper.toResponse(service.update(id, req));
    }

    @Operation(summary = "Soft-delete a person")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.softDelete(id); }
}
