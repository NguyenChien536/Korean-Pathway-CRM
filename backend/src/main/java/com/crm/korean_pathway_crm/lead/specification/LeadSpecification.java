package com.crm.korean_pathway_crm.lead.specification;

import com.crm.korean_pathway_crm.lead.dto.request.LeadFilterRequest;
import com.crm.korean_pathway_crm.lead.entity.Lead;
import com.crm.korean_pathway_crm.person.entity.Person;
import com.crm.korean_pathway_crm.person.entity.PersonContact;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class LeadSpecification {
    private LeadSpecification() {
    }

    public static Specification<Lead> of(LeadFilterRequest filter, UUID actorId, boolean canReadAll) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            Subquery<UUID> activePersons = query.subquery(UUID.class);
            Root<Person> activePerson = activePersons.from(Person.class);
            activePersons.select(activePerson.get("id"))
                    .where(cb.isNull(activePerson.get("deletedAt")));
            predicates.add(root.get("personId").in(activePersons));

            // Phạm vi bắt buộc được áp dụng ngay trong SQL, trước khi đếm và phân trang.
            if (!canReadAll) {
                predicates.add(cb.equal(root.get("assignedTo"), actorId));
            }
            if (filter.getAssignedTo() != null) {
                predicates.add(cb.equal(root.get("assignedTo"), filter.getAssignedTo()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPipelineId() != null) {
                predicates.add(cb.equal(root.get("pipelineId"), filter.getPipelineId()));
            }
            if (filter.getStageId() != null) {
                predicates.add(cb.equal(root.get("stageId"), filter.getStageId()));
            }
            if (filter.getSourceId() != null) {
                predicates.add(cb.equal(root.get("sourceId"), filter.getSourceId()));
            }

            if (StringUtils.hasText(filter.getSearch())) {
                String term = filter.getSearch().strip().toLowerCase(Locale.ROOT);
                String pattern = "%" + escapeLike(term) + "%";

                // Subquery tránh nhân đôi Lead khi một Person có nhiều số điện thoại/email.
                Subquery<UUID> nameMatches = query.subquery(UUID.class);
                Root<Person> person = nameMatches.from(Person.class);
                nameMatches.select(person.get("id")).where(
                        cb.isNull(person.get("deletedAt")),
                        cb.like(cb.lower(person.get("fullName")), pattern, '\\'));

                Subquery<UUID> contactMatches = query.subquery(UUID.class);
                Root<PersonContact> contact = contactMatches.from(PersonContact.class);
                Predicate contactValue = cb.or(
                        cb.like(cb.lower(contact.get("value")), pattern, '\\'),
                        cb.like(cb.lower(contact.get("normalizedValue")), pattern, '\\'));
                if (term.matches("0[0-9]+")) {
                    String internationalPattern = "%" + escapeLike("+84" + term.substring(1)) + "%";
                    contactValue = cb.or(contactValue,
                            cb.like(cb.lower(contact.get("normalizedValue")), internationalPattern, '\\'));
                }
                contactMatches.select(contact.get("person").get("id")).where(
                        cb.isNull(contact.get("person").get("deletedAt")), contactValue);

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("campaignName")), pattern, '\\'),
                        root.get("personId").in(nameMatches),
                        root.get("personId").in(contactMatches)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
