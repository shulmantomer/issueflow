package com.att.tdp.issueflow.security;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenDenyListRepository extends JpaRepository<TokenDenyListEntry, String> {

    @Modifying
    @Query("DELETE FROM TokenDenyListEntry t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
