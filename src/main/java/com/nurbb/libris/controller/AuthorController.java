package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.request.AuthorRequest;
import com.nurbb.libris.model.dto.response.AuthorResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.mapper.AuthorMapper;
import com.nurbb.libris.service.AuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Author Management", description = "Endpoints for managing author records")
public class AuthorController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    @Operation(
            summary = "Add a new author",
            description = "Creates a new author. Only accessible to librarians.",
            requestBody = @RequestBody(
                    description = "Author data to be created",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AuthorRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Author successfully created",
                            content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input provided")
            }
    )
    public ResponseEntity<AuthorResponse> createAuthor(
            @Valid @org.springframework.web.bind.annotation.RequestBody AuthorRequest request) {
        Author author = authorService.createAuthor(request.getName());
        return ResponseEntity.ok(authorMapper.toResponse(author));
    }

    @GetMapping
    @Operation(
            summary = "Get all authors",
            description = "Retrieves a list of all authors in the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authors retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuthorResponse.class))))
            }
    )
    public ResponseEntity<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> responses = authorService.getAllAuthors().stream()
                .map(authorMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get author by ID",
            description = "Retrieves details of a specific author using their UUID.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the author to retrieve", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Author found",
                            content = @Content(schema = @Schema(implementation = AuthorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Author not found")
            }
    )
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable UUID id) {
        return authorService.getAuthorById(id)
                .map(authorMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    /**
     * Deletes an author by UUID. Only accessible by librarians.
     * If the author has assigned books, deletion will fail.
     */

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete author",
            description = "Deletes an author from the system by UUID. Only librarians can perform this operation.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the author to delete", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Author deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Author not found")
            }
    )
    public ResponseEntity<String> deleteAuthor(@PathVariable UUID id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok("Author deleted successfully.");
    }

}
