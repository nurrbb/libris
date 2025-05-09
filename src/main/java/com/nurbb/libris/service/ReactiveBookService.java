package com.nurbb.libris.service;

import com.nurbb.libris.model.entity.Book;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReactiveBookService {
    Flux<Book> streamAllBooks();
    Mono<Book> getBookById(UUID id);
}
