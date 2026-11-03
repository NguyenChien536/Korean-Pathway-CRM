package com.crm.korean_pathway_crm.user.repository;

import com.crm.korean_pathway_crm.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
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
}
