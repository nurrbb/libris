package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.request.AuthorRequest;
import com.nurbb.libris.model.dto.response.AuthorResponse;
import com.nurbb.libris.model.entity.Author;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {UUID.class, Collectors.class})
public interface AuthorMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    Author toEntity(AuthorRequest request);

    @Mapping(target = "books", expression = "java(author.getBooks().stream().map(Book::getTitle).collect(Collectors.toSet()))")
    AuthorResponse toResponse(Author author);

}
