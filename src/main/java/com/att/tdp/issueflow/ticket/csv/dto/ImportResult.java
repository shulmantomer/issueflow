package com.att.tdp.issueflow.ticket.csv.dto;

import java.util.List;

public record ImportResult(int created, int failed, List<ImportError> errors) {}
