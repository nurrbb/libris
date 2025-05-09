package com.nurbb.libris.model.dto.request;

import com.nurbb.libris.model.entity.Genre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookRequest {

    @NotBlank
    private String title;

    @NotNull
    private String authorName;

    @NotBlank
    private String isbn;

    @NotNull
    private LocalDate publishedDate;

    @NotNull
    private Genre genre;

    @Min(value = 1, message = "Page count must be at least 1")
    private int pageCount;


    @NotNull
    private Integer count;
}
