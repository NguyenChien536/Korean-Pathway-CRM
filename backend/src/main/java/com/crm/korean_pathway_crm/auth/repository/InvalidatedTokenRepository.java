package com.crm.korean_pathway_crm.auth.repository;

import com.crm.korean_pathway_crm.auth.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    @Modifying
    @Query("DELETE FROM InvalidatedToken token WHERE token.expiryTime <= :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
