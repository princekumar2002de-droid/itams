package com.princekumar.itams.person.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonCreateRequest(
    @NotBlank @Size(max = 80)  String firstName,
    @NotBlank @Size(max = 80)  String lastName,
    @NotBlank @Email @Size(max = 160) String email,
    @Size(max = 40)            String phone
) {}
