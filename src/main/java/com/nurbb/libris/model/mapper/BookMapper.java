package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "author", expression = "java(author)")
    @Mapping(target = "isAvailable", ignore = true)
    @Mapping(target = "pageCount", source = "pageCount") // ✅ pageCount eklendi
    Book toEntity(BookRequest request, @Context Author author);

    @AfterMapping
    default void setAvailability(@MappingTarget Book book, BookRequest request) {
        book.setAvailable(request.getCount() != null && request.getCount() > 0);
    }

    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "createdAt", expression = "java(book.getCreatedDate().toString())")
    @Mapping(target = "updatedAt", expression = "java(book.getUpdatedDate().toString())")
    @Mapping(target = "pageCount", source = "pageCount") // ✅ pageCount eklendi
    BookResponse toResponse(Book book);
}
