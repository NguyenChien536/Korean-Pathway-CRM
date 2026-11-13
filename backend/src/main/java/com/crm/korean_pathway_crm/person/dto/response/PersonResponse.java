package com.crm.korean_pathway_crm.person.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PersonResponse {
    UUID id;
    String fullName;
    LocalDate dateOfBirth;
    String gender;
    String nationality;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    UUID createdById;
    UUID updatedById;
}
