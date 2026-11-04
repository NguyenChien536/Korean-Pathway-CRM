package com.crm.korean_pathway_crm.user.specification;

import com.crm.korean_pathway_crm.user.dto.request.UserFilterRequest;
import com.crm.korean_pathway_crm.user.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds optional database predicates for the user-list endpoint. */
public class UserSpecification implements Specification<User> {
    /** Request values are used as query values only; they never become SQL fragments. */
    private final UserFilterRequest filter;

    private UserSpecification(UserFilterRequest filter) {
        this.filter = filter;
    }

    public static Specification<User> of(UserFilterRequest filter) {
        return new UserSpecification(filter);
    }

    @Override
    public @Nullable Predicate toPredicate(
            Root<User> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        // This is JPA Criteria Predicate (a database condition), not java.util.function.Predicate.
        List<Predicate> predicates = new ArrayList<>();

        // A keyword may match any of the searchable text columns, so these conditions are OR-ed.
        if (StringUtils.hasText(filter.getSearch())) {
            String term = filter.getSearch().trim().toLowerCase(Locale.ROOT);
            String pattern = "%" + escapeLikeWildcards(term) + "%";

            Predicate byUsername = cb.like(cb.lower(root.get("username")), pattern, '\\');
            Predicate byFullName = cb.like(cb.lower(root.get("fullName")), pattern, '\\');
            Predicate byEmail = cb.like(cb.lower(root.get("email")), pattern, '\\');
            Predicate byPhone = cb.like(cb.lower(root.get("phone")), pattern, '\\');
            predicates.add(cb.or(byUsername, byFullName, byEmail, byPhone));
        }

        // The API calls this filter "status"; the persisted User field is named "isActive".
        // Null means the caller did not ask to filter by active state.
        if (filter.getStatus() != null) {
            predicates.add(cb.equal(root.get("isActive"), filter.getStatus()));
        }

        // role is a required to-one association, so comparing its UUID does not duplicate User rows.
        if (filter.getRoleId() != null) {
            predicates.add(cb.equal(root.get("role").get("id"), filter.getRoleId()));
        }

        // AND combines independent filters: the user must satisfy search, status, and role when present.
        // With no filters, an empty conjunction matches every User; pagination is still applied by Pageable.
        return cb.and(predicates.toArray(Predicate[]::new));
    }

    /** Treat SQL LIKE wildcards in user input as literal characters. */
    private String escapeLikeWildcards(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
