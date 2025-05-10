package com.nurbb.libris.reactive;

import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class BookAvailabilityPublisher {

    private final Sinks.Many<BookAvailabilityResponse> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(BookAvailabilityResponse response) {
        sink.tryEmitNext(response);
    }

    public Flux<BookAvailabilityResponse> getStream() {
        return sink.asFlux();
    }
}

