package com.att.tdp.issueflow.user;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.project.ProjectRepository;
import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.dto.CreateUserResponse;
import com.att.tdp.issueflow.user.dto.UpdateUserRequest;
import com.att.tdp.issueflow.user.dto.UserResponse;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User registry CRUD. Enforces case-insensitive username uniqueness, generates
 * a one-time password when none is provided on create (returned once as
 * {@code generatedPassword}), and refuses to delete a user who still owns
 * active projects.
 */
@Service
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int GENERATED_PASSWORD_LENGTH = 16;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditPublisher auditPublisher;

    public UserService(UserRepository userRepository,
                       ProjectRepository projectRepository,
                       PasswordEncoder passwordEncoder,
                       AuditPublisher auditPublisher) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditPublisher = auditPublisher;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(getUserOrThrow(id));
    }

    @Transactional
    public CreateUserResponse create(CreateUserRequest request, Long actorId) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email '" + request.email() + "' is already registered");
        }

        String rawPassword = request.password();
        String generatedPassword = null;
        if (rawPassword == null || rawPassword.isBlank()) {
            generatedPassword = generatePassword();
            rawPassword = generatedPassword;
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .fullName(request.fullName())
                .role(request.role())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        User saved = userRepository.save(user);

        auditPublisher.publish(AuditAction.CREATE, AuditEntityType.USER, saved.getId(),
                AuditActor.USER, actorId);
        return CreateUserResponse.from(saved, generatedPassword);
    }

    @Transactional
    public void update(Long id, UpdateUserRequest request, Long actorId) {
        User user = getUserOrThrow(id);
        user.setFullName(request.fullName());
        user.setRole(request.role());
        userRepository.save(user);
        auditPublisher.publish(AuditAction.UPDATE, AuditEntityType.USER, id,
                AuditActor.USER, actorId);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        User user = getUserOrThrow(id);
        long ownedProjects = projectRepository.countByOwnerId(id);
        if (ownedProjects > 0) {
            throw new ConflictException("Cannot delete user " + id + ": the user owns "
                    + ownedProjects + " active project(s). Reassign project ownership first.");
        }
        userRepository.delete(user);
        auditPublisher.publish(AuditAction.DELETE, AuditEntityType.USER, id,
                AuditActor.USER, actorId);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
