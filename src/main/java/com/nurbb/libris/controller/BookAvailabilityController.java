package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/reactive/books")
@RequiredArgsConstructor
@Tag(name = "Reactive Book Availability", description = "Real-time updates of book availability")
public class BookAvailabilityController {

    private final BookAvailabilityPublisher publisher;

    @GetMapping(value = "/availability", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream real-time book availability updates",
            description = "Provides a Server-Sent Events (SSE) stream of book availability changes. Useful for reactive UIs to get live updates when books are borrowed or returned."
    )
    public Flux<BookAvailabilityResponse> streamAvailability() {
        return publisher.getStream();
    }
}

