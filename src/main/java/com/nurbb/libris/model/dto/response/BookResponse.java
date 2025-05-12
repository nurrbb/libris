package com.nurbb.libris.model.dto.response;

import com.nurbb.libris.model.entity.valueobject.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class BookResponse {

    private UUID id;
    private String title;
    private String authorName;

    private String isbn;
    private LocalDate publishedDate;
    private Genre genre;
    private Integer count;
    private int pageCount;
    private boolean isAvailable;

    private String createdAt;
    private String updatedAt;
}
