package com.nurbb.libris.model.dto.response;

import com.nurbb.libris.model.entity.Genre;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record BookResponseDTO(
        UUID id,
        String title,
        Set<String> authors,
        String isbn,
        LocalDate publishedDate,
        Genre genre,
        Integer count,
        boolean isAvailable,
        String createdAt,
        String updatedAt
) {}