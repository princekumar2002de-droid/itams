package com.princekumar.itams.person.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** All fields nullable; {@code null} means "leave unchanged". */
public record PersonUpdateRequest(
    @Size(max = 80)  String firstName,
    @Size(max = 80)  String lastName,
    @Email @Size(max = 160) String email,
    @Size(max = 40)  String phone,
    Boolean active
) {}
