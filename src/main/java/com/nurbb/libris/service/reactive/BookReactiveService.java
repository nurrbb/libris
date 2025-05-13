package com.nurbb.libris.service.reactive;


import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class BookReactiveService {

    private final BookService bookService;

    public Flux<BookResponse> searchBooksReactively(String query, int page, int size) {
        return Flux.defer(() -> {
            System.out.println("[REACTIVE SEARCH] Query: " + query + ", page: " + page + ", size: " + size);
            Page<BookResponse> pageResult = bookService.searchBooks(query, page, size);
            return Flux.fromIterable(pageResult.getContent());
        });
    }


}
