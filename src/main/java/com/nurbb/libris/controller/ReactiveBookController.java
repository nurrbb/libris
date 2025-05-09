package com.nurbb.libris.controller;

import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.util.BookAvailabilityPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/reactive/books")
@RequiredArgsConstructor
@Tag(name = "Reactive Book Availability", description = "Reactive stream for real-time book availability updates")
public class ReactiveBookController {

    private final BookAvailabilityPublisher availabilityPublisher;

    @GetMapping(value = "/availability/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream real-time book availability updates",
            description = "Provides a continuous Server-Sent Events (SSE) stream of book availability updates. "
                    + "Useful for frontends to listen for live changes in book stock.")
    public Flux<Book> streamBookAvailability() {
        return availabilityPublisher.stream();
    }
}
