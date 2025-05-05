package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.entity.Author;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;
@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "authors", source = "authors", qualifiedByName = "mapAuthorNamesToEntities")
    @Mapping(target = "isAvailable", ignore = true)
    Book toEntity(BookRequest request, @Context Set<Author> authorEntities);

    @AfterMapping
    default void setAvailability(@MappingTarget Book book, BookRequest request) {
        book.setAvailable(request.getCount() != null && request.getCount() > 0);
    }

    @Mapping(target = "authorNames", source = "authors", qualifiedByName = "mapAuthorsToNames")
    @Mapping(target = "createdAt", expression = "java(book.getCreatedDate().toString())")
    @Mapping(target = "updatedAt", expression = "java(book.getUpdatedDate().toString())")
    BookResponse toResponse(Book book);

    @Named("mapAuthorsToNames")
    static Set<String> mapAuthorsToNames(Set<Author> authors) {
        return authors.stream()
                .map(Author::getName)
                .collect(Collectors.toSet());
    }

    @Named("mapAuthorNamesToEntities")
    static Set<Author> mapAuthorNamesToEntities(Set<String> names, @Context Set<Author> authorEntities) {
        return authorEntities.stream()
                .filter(author -> names.contains(author.getName()))
                .collect(Collectors.toSet());
    }
}
