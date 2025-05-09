package com.nurbb.libris.service.impl;

import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.service.ReactiveBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReactiveBookServiceImpl implements ReactiveBookService {

    private final BookRepository bookRepository;

    @Override
    public Flux<Book> streamAllBooks() {
        return Flux.defer(() -> Flux.fromIterable(bookRepository.findAll()))
                .delayElements(Duration.ofSeconds(3));
    }

    @Override
    public Mono<Book> getBookById(UUID id) {
        return Mono.justOrEmpty(bookRepository.findById(id));
    }
}
