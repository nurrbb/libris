package com.nurbb.libris.util;


import com.nurbb.libris.model.entity.Book;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class BookAvailabilityPublisher {

    private final Sinks.Many<Book> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(Book book) {
        sink.tryEmitNext(book);
    }

    public Flux<Book> stream() {
        return sink.asFlux();
    }
}