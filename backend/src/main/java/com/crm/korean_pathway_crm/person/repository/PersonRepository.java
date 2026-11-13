package com.crm.korean_pathway_crm.person.repository;

import com.crm.korean_pathway_crm.person.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
}
