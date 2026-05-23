package com.att.tdp.issueflow.ticket.attachment;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.ticket.attachment.dto.AttachmentResponse;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final AuditPublisher auditPublisher;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             TicketRepository ticketRepository,
                             AuditPublisher auditPublisher) {
        this.attachmentRepository = attachmentRepository;
        this.ticketRepository = ticketRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public AttachmentResponse upload(Long ticketId, MultipartFile file, Long actorId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new NotFoundException("Ticket " + ticketId + " not found");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Attachment file is required and must not be empty");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the uploaded file");
        }
        FileTypeValidator.validate(file.getContentType(), data);

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "attachment";
        }
        Attachment attachment = Attachment.builder()
                .ticketId(ticketId)
                .filename(filename)
                .contentType(file.getContentType())
                .sizeBytes(data.length)
                .data(data)
                .uploadedBy(actorId)
                .build();
        Attachment saved = attachmentRepository.save(attachment);
        auditPublisher.publish(AuditAction.CREATE, AuditEntityType.ATTACHMENT, saved.getId(),
                AuditActor.USER, actorId);
        return AttachmentResponse.from(saved);
    }

    @Transactional
    public void delete(Long ticketId, Long attachmentId, Long actorId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Attachment " + attachmentId + " not found"));
        if (!attachment.getTicketId().equals(ticketId)) {
            throw new NotFoundException(
                    "Attachment " + attachmentId + " not found on ticket " + ticketId);
        }
        attachmentRepository.delete(attachment);
        auditPublisher.publish(AuditAction.DELETE, AuditEntityType.ATTACHMENT, attachmentId,
                AuditActor.USER, actorId);
    }
}
