package com.princekumar.itams.person;

import com.princekumar.itams.person.dto.PersonResponse;

final class PersonMapper {
    private PersonMapper() {}
    static PersonResponse toResponse(Person p) {
        return new PersonResponse(
            p.getId(), p.getFirstName(), p.getLastName(),
            p.getEmail(), p.getPhone(), p.isActive(),
            p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
