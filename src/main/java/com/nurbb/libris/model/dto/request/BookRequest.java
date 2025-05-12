package com.nurbb.libris.model.dto.request;

import com.nurbb.libris.model.entity.valueobject.Genre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookRequest {


    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotNull(message = "Author name is required")
    private String authorName;

    @NotBlank(message = "ISBN must not be blank")
    private String isbn;

    @NotNull(message = "Published date is required")
    private LocalDate publishedDate;

    @NotNull(message = "Genre must be specified")
    private Genre genre;

    @Min(value = 1, message = "Page count must be at least 1")
    private int pageCount;

    @NotNull(message = "Book count must be specified")
    private Integer count;
}
