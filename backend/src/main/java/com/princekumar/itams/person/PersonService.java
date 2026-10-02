package com.princekumar.itams.person;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.person.dto.PersonCreateRequest;
import com.princekumar.itams.person.dto.PersonUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PersonService {

    private final PersonRepository repo;

    public PersonService(PersonRepository repo) { this.repo = repo; }

    @AuditWrite(entity = "Person", action = "CREATE")
    public Person create(PersonCreateRequest req) {
        if (repo.existsByEmailIgnoreCaseLive(req.email())) {
            throw new BusinessRuleViolationException(
                "person.email_already_used",
                "A person with email '%s' already exists.".formatted(req.email()));
        }
        return repo.save(new Person(req.firstName(), req.lastName(), req.email(), req.phone()));
    }

    @AuditWrite(entity = "Person", action = "UPDATE")
    public Person update(Long id, PersonUpdateRequest req) {
        Person p = requireLive(id);
        if (req.firstName() != null) p.setFirstName(req.firstName());
        if (req.lastName() != null)  p.setLastName(req.lastName());
        if (req.email() != null && !req.email().equalsIgnoreCase(p.getEmail())) {
            if (repo.existsByEmailIgnoreCaseLive(req.email())) {
                throw new BusinessRuleViolationException(
                    "person.email_already_used",
                    "A person with email '%s' already exists.".formatted(req.email()));
            }
            p.setEmail(req.email());
        }
        if (req.phone() != null)  p.setPhone(req.phone());
        if (req.active() != null) p.setActive(req.active());
        return p;
    }

    public void softDelete(Long id) { requireLive(id).softDelete(); }

    @Transactional(readOnly = true)
    public Person findById(Long id) { return requireLive(id); }

    @Transactional(readOnly = true)
    public Page<Person> search(String q, Pageable pageable) { return repo.searchLive(q, pageable); }

    /** Public helper for other services that need to load a live person. */
    Person requireLive(Long id) {
        return repo.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Person", id));
    }
}
