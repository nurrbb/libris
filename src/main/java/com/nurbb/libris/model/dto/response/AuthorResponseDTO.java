package com.nurbb.libris.model.dto.response;

import java.util.Set;
import java.util.UUID;

public record AuthorResponseDTO(
        UUID id,
        String name,
        Set<String> books
) {}