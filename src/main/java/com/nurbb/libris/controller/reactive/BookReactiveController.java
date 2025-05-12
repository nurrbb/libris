package com.nurbb.libris.controller.reactive;

import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.service.reactive.BookReactiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/reactive/books")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reactive Book Operations", description = "Reactive endpoints for book search and real-time updates using WebFlux")
public class BookReactiveController {

    private final BookReactiveService bookReactiveService;
    private final BookAvailabilityPublisher publisher;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON', 'GUEST')")
    @Operation(
            summary = "Search books reactively",
            description = "Search books by title, author name, or ISBN. Returns results reactively using WebFlux.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Books found",
                            content = @Content(schema = @Schema(implementation = BookResponse.class)))
            }
    )
    public Flux<BookResponse> searchBooks(
            @Parameter(description = "Search keyword (title, author name, or ISBN)")
            @RequestParam String query,

            @Parameter(description = "Page number (starting from 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookReactiveService.searchBooksReactively(query, page, size);
    }

    @GetMapping(value = "/availability/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'PATRON', 'GUEST')")
    @Operation(
            summary = "Stream book availability updates",
            description = "Streams real-time updates of book availability using Server-Sent Events (SSE).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Streaming book availability",
                            content = @Content(schema = @Schema(implementation = BookAvailabilityResponse.class)))
            }
    )
    public Flux<BookAvailabilityResponse> streamBookAvailability() {
        return publisher.getStream();
    }
}
