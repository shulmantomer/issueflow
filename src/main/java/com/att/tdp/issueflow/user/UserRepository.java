package com.att.tdp.issueflow.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    @Query(value = """
            SELECT u.id, u.username, COUNT(t.id)
            FROM users u
            LEFT JOIN tickets t
              ON t.assignee_id = u.id
              AND t.project_id = :projectId
              AND t.status <> 'DONE'
              AND t.deleted_at IS NULL
            WHERE u.role = 'DEVELOPER'
            GROUP BY u.id, u.username
            ORDER BY COUNT(t.id) ASC, u.id ASC
            """, nativeQuery = true)
    List<Object[]> findDeveloperWorkload(@Param("projectId") Long projectId);
}
