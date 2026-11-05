package com.shulmantomer.issueflow.ticket.attachment.dto;

import com.shulmantomer.issueflow.ticket.attachment.Attachment;

public record AttachmentResponse(Long id, Long ticketId, String filename, String contentType) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getTicketId(),
                attachment.getFilename(), attachment.getContentType());
    }
}
