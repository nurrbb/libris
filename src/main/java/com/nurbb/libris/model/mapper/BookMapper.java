package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.awt.print.Book;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "authors", source = "authors")
    @Mapping(target = "isAvailable", expression = "java(request.count() > 0)")
    Book toEntity(BookRequest request, @Context Set<Author> authors);

    @Mapping(target = "authors", expression = "java(book.getAuthors().stream().map(Author::getName).collect(Collectors.toSet()))")
    @Mapping(target = "createdAt", expression = "java(book.getCreatedDate().toString())")
    @Mapping(target = "updatedAt", expression = "java(book.getUpdatedDate().toString())")
    BookResponse toResponse(Book book);
}