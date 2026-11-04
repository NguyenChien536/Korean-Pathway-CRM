package com.crm.korean_pathway_crm.user.repository;

import com.crm.korean_pathway_crm.user.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByRoleId(UUID roleId);
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);
    Optional<User> findByUsernameIgnoreCase(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE lower(u.username) = lower(:username)")
    Optional<User> findByUsernameIgnoreCaseForUpdate(@Param("username") String username);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(UUID id);

    /**
     * Queries only the requested page and loads the to-one role used by UserResponse mapping.
     * Do not fetch the role-permissions collection in this paged query.
     */
    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Specification<User> specification, Pageable pageable);
}
