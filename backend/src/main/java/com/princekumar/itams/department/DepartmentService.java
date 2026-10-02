package com.princekumar.itams.department;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.department.dto.DepartmentCreateRequest;
import com.princekumar.itams.department.dto.DepartmentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for departments. Sits between the controller (HTTP
 * shape) and the repository (persistence). Every state change is
 * transactional; every business-rule violation throws a typed
 * exception that {@code GlobalExceptionHandler} maps to a stable HTTP
 * response.
 */
@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository repo;

    public DepartmentService(DepartmentRepository repo) {
        this.repo = repo;
    }

    // ── commands ─────────────────────────────────────────────────────────────

    @AuditWrite(entity = "Department", action = "CREATE")
    public Department create(DepartmentCreateRequest req) {
        if (repo.existsByCodeIgnoreCaseAndDeletedAtIsNull(req.code())) {
            throw new BusinessRuleViolationException(
                "department.code_already_used",
                "A department with code '%s' already exists.".formatted(req.code()));
        }

        Department parent = resolveParent(req.parentDepartmentId(), null);
        Department d = new Department(req.code(), req.name(), parent, req.managerPersonId());
        return repo.save(d);
    }

    @AuditWrite(entity = "Department", action = "UPDATE")
    public Department update(Long id, DepartmentUpdateRequest req) {
        Department d = requireLive(id);

        if (req.name() != null) {
            d.setName(req.name());
        }
        if (req.parentDepartmentId() != null) {
            d.setParentDepartment(resolveParent(req.parentDepartmentId(), id));
        }
        if (req.managerPersonId() != null) {
            // Not looked up here; the DB foreign key refuses an unknown person id.
            d.setManagerPersonId(req.managerPersonId());
        }
        return d;
    }

    public void softDelete(Long id) {
        Department d = requireLive(id);
        d.softDelete();
    }

    // ── queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Department findById(Long id) {
        return requireLive(id);
    }

    @Transactional(readOnly = true)
    public Page<Department> search(String q, Pageable pageable) {
        return repo.searchLive(q, pageable);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Department requireLive(Long id) {
        return repo.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    /**
     * Loads the referenced parent, rejecting self-parenting.
     * (A full cycle check would walk the ancestor chain — deferred to a
     * later hardening pass; the DB will not catch cycles for us.)
     */
    private Department resolveParent(Long parentId, Long selfId) {
        if (parentId == null) return null;
        if (selfId != null && parentId.equals(selfId)) {
            throw new BusinessRuleViolationException(
                "department.self_parent",
                "A department cannot be its own parent.");
        }
        return repo.findByIdAndDeletedAtIsNull(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent Department", parentId));
    }
}
