package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.request.AuthorRequest;
import com.nurbb.libris.model.dto.response.AuthorResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.mapper.AuthorMapper;
import com.nurbb.libris.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
@Tag(name = "Author Management", description = "Endpoints for managing authors")
public class AuthorController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    @Operation(summary = "Add a new author", responses = {
            @ApiResponse(responseCode = "200", description = "Author successfully created",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class)))
    })
    public ResponseEntity<AuthorResponse> createAuthor(@RequestBody @Valid AuthorRequest request) {
        Author author = authorService.createAuthor(request.getName());
        return ResponseEntity.ok(authorMapper.toResponse(author));
    }

    @GetMapping
    @Operation(summary = "Get all authors", responses = {
            @ApiResponse(responseCode = "200", description = "Authors retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthorResponse.class))))
    })
    public ResponseEntity<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> responses = authorService.getAllAuthors().stream()
                .map(authorMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get author by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Author found",
                    content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable UUID id) {
        return authorService.getAuthorById(id)
                .map(authorMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an author by ID")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }

}
