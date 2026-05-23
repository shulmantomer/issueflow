package com.att.tdp.issueflow.ticket.csv;

import com.att.tdp.issueflow.common.enums.TicketPriority;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.common.enums.TicketType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.project.ProjectRepository;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.ticket.csv.dto.ImportError;
import com.att.tdp.issueflow.ticket.csv.dto.ImportResult;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketCsvService {

    private static final String[] HEADERS =
            {"id", "title", "description", "status", "priority", "type", "assigneeId"};
    private static final List<String> REQUIRED_HEADERS =
            List.of("title", "status", "priority", "type");

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final TicketService ticketService;

    public TicketCsvService(TicketRepository ticketRepository,
                            ProjectRepository projectRepository,
                            TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public byte[] export(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project " + projectId + " not found");
        }
        List<Ticket> tickets = ticketRepository.findByProjectIdOrderById(projectId);
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out,
                CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {
            for (Ticket t : tickets) {
                printer.printRecord(t.getId(), t.getTitle(), t.getDescription(),
                        t.getStatus(), t.getPriority(), t.getType(), t.getAssigneeId());
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate the CSV export");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    public ImportResult importTickets(Long projectId, MultipartFile file, Long actorId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BadRequestException("Project " + projectId + " does not exist");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required and must not be empty");
        }

        List<ImportError> errors = new ArrayList<>();
        int created = 0;
        int failed = 0;
        int rowNum = 0;

        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder()
                        .setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            requireHeaders(parser);

            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    ticketService.create(parseRow(record, projectId), actorId);
                    created++;
                } catch (Exception e) {
                    failed++;
                    errors.add(new ImportError(rowNum, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the CSV file");
        }
        return new ImportResult(created, failed, errors);
    }

    private void requireHeaders(CSVParser parser) {
        List<String> headers = parser.getHeaderNames();
        for (String required : REQUIRED_HEADERS) {
            if (!headers.contains(required)) {
                throw new BadRequestException(
                        "CSV is missing required column '" + required + "'");
            }
        }
    }

    private CreateTicketRequest parseRow(CSVRecord record, Long projectId) {
        String title = get(record, "title");
        if (title == null || title.isBlank()) {
            throw new BadRequestException("title is required");
        }
        return new CreateTicketRequest(
                title,
                get(record, "description"),
                parseEnum(TicketStatus.class, get(record, "status"), "status"),
                parseEnum(TicketPriority.class, get(record, "priority"), "priority"),
                parseEnum(TicketType.class, get(record, "type"), "type"),
                projectId,
                parseAssigneeId(get(record, "assigneeId")),
                null);
    }

    private String get(CSVRecord record, String column) {
        return record.isMapped(column) && record.isSet(column) ? record.get(column) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid " + field + " '" + value + "'");
        }
    }

    private Long parseAssigneeId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("invalid assigneeId '" + value + "'");
        }
    }
}
