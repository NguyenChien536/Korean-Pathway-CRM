package com.crm.korean_pathway_crm.user.specification;

import com.crm.korean_pathway_crm.role.entity.Role;
import com.crm.korean_pathway_crm.user.dto.request.UserFilterRequest;
import com.crm.korean_pathway_crm.user.entity.User;
import jakarta.persistence.criteria.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class UserSpecification implements Specification<User> {
    private final UserFilterRequest filter;

    private UserSpecification(UserFilterRequest filter) {
        this.filter = filter;
    }

    public static Specification<User> of(UserFilterRequest filter) {
        return new UserSpecification(filter);
    }
    @Override
    public jakarta.persistence.criteria.@Nullable Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb){
        List<Predicate> predicates = new ArrayList<>();

        // ── 1. Search: LIKE trên username, email, phone ──────────────────────
        if (StringUtils.hasText(filter.getSearch())) {
            String keyword = "%" + filter.getSearch().toLowerCase() + "%";
            jakarta.persistence.criteria.Predicate byUsername = cb.like(cb.lower(root.get("username")), keyword);
            jakarta.persistence.criteria.Predicate byFullName = cb.like(cb.lower(root.get("fullName")),keyword);
            jakarta.persistence.criteria.Predicate byEmail    = cb.like(cb.lower(root.get("email")), keyword);
            jakarta.persistence.criteria.Predicate byPhone    = cb.like(cb.lower(root.get("phone")), keyword);
            predicates.add( cb.or(byUsername,byFullName, byEmail, byPhone));
        }

        // ── 2. Filter: status (ACTIVE, LOCKED, INACTIVE) ─────────────────────
        if (filter.getStatus() != null) {
            predicates.add( cb.equal(root.get("isActive"), filter.getStatus()));
        }
        // ── 3. Filter: roleId (JOIN vào bảng roles) ──────────────────────────
        if (StringUtils.hasText(String.valueOf(filter.getRoleId()))) {
            Join<User, Role> roleJoin = root.join("role", JoinType.INNER);
            predicates.add( cb.equal(roleJoin.get("id"), filter.getRoleId()));
        }

        query.distinct(true);

        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    }
}
