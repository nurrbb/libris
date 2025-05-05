package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.request.AuthorRequest;
import com.nurbb.libris.model.dto.response.AuthorResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "books", ignore = true)
    Author toEntity(AuthorRequest request);

    @Mapping(target = "books", source = "books", qualifiedByName = "mapBooksToNames")
    AuthorResponse toResponse(Author author);

    @Named("mapBooksToNames")
    static Set<String> mapBooksToNames(List<Book> books) {
        if (books == null) return Set.of();
        return books.stream()
                .map(Book::getTitle)
                .collect(Collectors.toSet());
    }
}
