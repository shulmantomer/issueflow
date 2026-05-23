package com.att.tdp.issueflow.ticket.csv;

import com.att.tdp.issueflow.security.AuthenticatedUser;
import com.att.tdp.issueflow.ticket.csv.dto.ImportResult;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
public class TicketCsvController {

    private final TicketCsvService ticketCsvService;

    public TicketCsvController(TicketCsvService ticketCsvService) {
        this.ticketCsvService = ticketCsvService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam Long projectId) {
        byte[] csv = ticketCsvService.export(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tickets-project-" + projectId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResult> importTickets(
            @RequestParam Long projectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(
                ticketCsvService.importTickets(projectId, file, principal.id()));
    }
}
