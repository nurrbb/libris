package com.nurbb.libris.model.dto.response;

import java.util.Set;
import java.util.UUID;

public record AuthorResponse(
        UUID id,
        String name,
        Set<String> books
) {}