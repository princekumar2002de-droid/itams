package com.princekumar.itams.person;

import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.person.dto.PersonCreateRequest;
import com.princekumar.itams.person.dto.PersonUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock PersonRepository repo;
    @InjectMocks PersonService service;

    @Test
    void create_persists_when_email_free() {
        var req = new PersonCreateRequest("Ada", "Lovelace", "ada@example.com", null);
        when(repo.existsByEmailIgnoreCaseLive("ada@example.com")).thenReturn(false);
        when(repo.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        Person p = service.create(req);
        assertThat(p.getFirstName()).isEqualTo("Ada");
        assertThat(p.isActive()).isTrue();
    }

    @Test
    void create_rejects_duplicate_email() {
        var req = new PersonCreateRequest("Ada", "L", "ada@example.com", null);
        when(repo.existsByEmailIgnoreCaseLive("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void findById_missing_throws_not_found() {
        when(repo.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_email_case_insensitive_no_conflict_with_self() {
        Person p = new Person("Ada", "L", "ada@example.com", null);
        when(repo.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(p));

        // Same email in different case → not a conflict.
        service.update(5L, new PersonUpdateRequest(null, null, "ADA@example.com", null, null));
        assertThat(p.getEmail()).isEqualToIgnoringCase("ada@example.com");
    }

    @Test
    void softDelete_marks_row_deleted() {
        Person p = new Person("Ada", "L", "ada@example.com", null);
        when(repo.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(p));
        service.softDelete(1L);
        assertThat(p.isDeleted()).isTrue();
    }
}
