package com.crm.korean_pathway_crm.person.repository;

import com.crm.korean_pathway_crm.person.entity.PersonContact;
import com.crm.korean_pathway_crm.person.enums.PersonContactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PersonContactRepository extends JpaRepository<PersonContact, UUID> {
    List<PersonContact> findAllByContactTypeAndNormalizedValue(
            PersonContactType personContactType,
            String normalizedValue
    );
}
