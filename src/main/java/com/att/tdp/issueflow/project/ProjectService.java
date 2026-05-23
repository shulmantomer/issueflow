package com.att.tdp.issueflow.project;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.project.dto.WorkloadEntry;
import com.att.tdp.issueflow.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher;

    public ProjectService(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          AuditPublisher auditPublisher) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(getProjectOrThrow(id));
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long actorId) {
        if (!userRepository.existsById(request.ownerId())) {
            throw new BadRequestException("Owner user " + request.ownerId() + " does not exist");
        }
        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(request.ownerId())
                .build();
        Project saved = projectRepository.save(project);
        auditPublisher.publish(AuditAction.CREATE, AuditEntityType.PROJECT, saved.getId(),
                AuditActor.USER, actorId);
        return ProjectResponse.from(saved);
    }

    @Transactional
    public void update(Long id, UpdateProjectRequest request, Long actorId) {
        Project project = getProjectOrThrow(id);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Project name must not be blank");
            }
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        projectRepository.save(project);
        auditPublisher.publish(AuditAction.UPDATE, AuditEntityType.PROJECT, id,
                AuditActor.USER, actorId);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Project project = getProjectOrThrow(id);
        // Soft delete only — set the tombstone, never projectRepository.delete().
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
        auditPublisher.publish(AuditAction.DELETE, AuditEntityType.PROJECT, id,
                AuditActor.USER, actorId);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getDeleted() {
        return projectRepository.findAllDeleted().stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public void restore(Long id, Long actorId) {
        Project project = projectRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("Soft-deleted project " + id + " not found"));
        project.setDeletedAt(null);
        projectRepository.save(project);
        auditPublisher.publish(AuditAction.RESTORE, AuditEntityType.PROJECT, id,
                AuditActor.USER, actorId);
    }

    @Transactional(readOnly = true)
    public List<WorkloadEntry> getWorkload(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project " + projectId + " not found");
        }
        return userRepository.findDeveloperWorkload(projectId).stream()
                .map(row -> new WorkloadEntry(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project " + id + " not found"));
    }
}
