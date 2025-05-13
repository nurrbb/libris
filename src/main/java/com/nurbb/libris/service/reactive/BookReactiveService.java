package com.nurbb.libris.service.reactive;

import com.nurbb.libris.model.dto.response.BookAvailabilityResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.reactive.BookAvailabilityPublisher;
import com.nurbb.libris.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class BookReactiveService {

    private final BookService bookService;
    private final BookAvailabilityPublisher publisher;

    public Flux<BookResponse> searchBooksReactively(String query, int page, int size) {
        return Flux.defer(() -> {
            Page<BookResponse> pageResult = bookService.searchBooks(query, page, size);

            pageResult.getContent().forEach(book -> publisher.publish(
                    BookAvailabilityResponse.builder()
                            .bookId(book.getId())
                            .title(book.getTitle())
                            .isAvailable(book.isAvailable())
                            .build()
            ));

            return Flux.fromIterable(pageResult.getContent());
        });
    }
}
