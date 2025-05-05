package com.nurbb.libris.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequest {

    @NotNull
    private UUID bookId; //name?

    @NotNull
    private String email;

    @NotNull
    private LocalDate borrowDate;

    @NotNull
    private LocalDate dueDate;
}
