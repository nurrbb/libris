package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookDeleteResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.service.BookService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Book Management", description = "Endpoints for managing book records including add, update, delete, search and retrieval operations.")
public class BookController {

    private final BookService bookService;

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PostMapping
    @Operation(
            summary = "Add a new book",
            description = "Allows librarians to add a new book with details such as title, author, ISBN, publication date, and genre.",
            requestBody = @RequestBody(
                    description = "Book data to be created",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Book successfully created",
                            content = @Content(schema = @Schema(implementation = BookResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input provided")
            }
    )
    public ResponseEntity<BookResponse> addBook(@Valid @org.springframework.web.bind.annotation.RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.addBook(request));
    }

    @PreAuthorize("hasAnyRole('GUEST', 'PATRON', 'LIBRARIAN')")
    @GetMapping
    @Operation(
            summary = "Retrieve all books",
            description = "Returns a list of all books in the system. Accessible to guests, patrons, and librarians.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Books successfully retrieved",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookResponse.class))))
            }
    )
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get book by ID",
            description = "Retrieves a book's details by its unique identifier.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the book to retrieve", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book found",
                            content = @Content(schema = @Schema(implementation = BookResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search books",
            description = "Search for books by title, author name, or ISBN. Results are paginated.",
            parameters = {
                    @Parameter(name = "query", description = "Search keyword for title, author, or ISBN", required = true),
                    @Parameter(name = "page", description = "Page number (0-based index)", required = false),
                    @Parameter(name = "size", description = "Number of results per page", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search results returned successfully",
                            content = @Content(schema = @Schema(implementation = BookResponse.class)))
            }
    )
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookService.searchBooks(query, page, size));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @PutMapping("/{id}")
    @Operation(
            summary = "Update book",
            description = "Updates the details of an existing book by ID. Only librarians can perform this operation.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the book to update", required = true)
            },
            requestBody = @RequestBody(
                    description = "Updated book information",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book updated successfully",
                            content = @Content(schema = @Schema(implementation = BookResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    public ResponseEntity<BookResponse> updateBook(@PathVariable UUID id, @Valid @org.springframework.web.bind.annotation.RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @PreAuthorize("hasRole('LIBRARIAN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete book",
            description = "Deletes a book from the system by its ID. Only accessible to librarians.",
            parameters = {
                    @Parameter(name = "id", description = "UUID of the book to delete", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Book deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Book not found")
            }
    )
    public ResponseEntity<BookDeleteResponse> deleteBook(@PathVariable UUID id) {
        BookDeleteResponse response = bookService.deleteBook(id);
        return ResponseEntity.ok(response);
    }
}
