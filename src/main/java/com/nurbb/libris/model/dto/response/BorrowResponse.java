package com.nurbb.libris.model.dto.response;


import java.time.LocalDate;
import java.util.UUID;

public record BorrowResponse(
        UUID id,
        String bookTitle,
        String userFullName,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean returned,
        String createdAt,
        String updatedAt
) {}