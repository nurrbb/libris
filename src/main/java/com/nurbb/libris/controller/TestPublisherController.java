package com.nurbb.libris.controller;

import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestPublisherController {

    private final BookAvailabilityPublisher publisher;

    @PostMapping("/trigger")
    public void triggerUpdate() {
        BookAvailabilityResponse response = BookAvailabilityResponse.builder()
                .bookId(UUID.randomUUID())
                .title("Test Book")
                .isAvailable(Math.random() > 0.5)
                .build();

        publisher.publish(response);
    }
}
