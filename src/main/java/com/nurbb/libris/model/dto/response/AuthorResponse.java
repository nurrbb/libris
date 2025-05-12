package com.nurbb.libris.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private UUID id;
    private String name;
    private Set<String> books;
}