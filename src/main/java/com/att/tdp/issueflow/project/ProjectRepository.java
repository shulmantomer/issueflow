package com.att.tdp.issueflow.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    long countByOwnerId(Long ownerId);

    @Query(value = "SELECT * FROM projects WHERE deleted_at IS NOT NULL ORDER BY id",
           nativeQuery = true)
    List<Project> findAllDeleted();

    @Query(value = "SELECT * FROM projects WHERE id = :id AND deleted_at IS NOT NULL",
           nativeQuery = true)
    Optional<Project> findDeletedById(@Param("id") Long id);
}
