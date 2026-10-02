package com.princekumar.itams.department;

import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.department.dto.DepartmentCreateRequest;
import com.princekumar.itams.department.dto.DepartmentUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests for {@link DepartmentService}. Uses Mockito on the
 * repository — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepository repo;
    @InjectMocks DepartmentService service;

    @Test
    void create_persists_when_code_free() {
        var req = new DepartmentCreateRequest("IT", "Information Technology", null, null);
        when(repo.existsByCodeIgnoreCaseAndDeletedAtIsNull("IT")).thenReturn(false);
        when(repo.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        Department created = service.create(req);

        assertThat(created.getCode()).isEqualTo("IT");
        assertThat(created.getName()).isEqualTo("Information Technology");
        assertThat(created.getParentDepartment()).isNull();
    }

    @Test
    void create_rejects_when_code_already_used() {
        var req = new DepartmentCreateRequest("IT", "IT", null, null);
        when(repo.existsByCodeIgnoreCaseAndDeletedAtIsNull("IT")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("IT");
    }

    @Test
    void create_with_missing_parent_throws_not_found() {
        var req = new DepartmentCreateRequest("IT", "IT", 999L, null);
        when(repo.existsByCodeIgnoreCaseAndDeletedAtIsNull("IT")).thenReturn(false);
        when(repo.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void update_rejects_self_parent() {
        Department d = new Department("IT", "IT", null, null);
        when(repo.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(d));

        var req = new DepartmentUpdateRequest(null, 5L, null);
        assertThatThrownBy(() -> service.update(5L, req))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("own parent");
    }

    @Test
    void findById_missing_throws_not_found() {
        when(repo.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDelete_marks_row_deleted() {
        Department d = new Department("IT", "IT", null, null);
        when(repo.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(d));

        service.softDelete(1L);

        assertThat(d.isDeleted()).isTrue();
    }
}
