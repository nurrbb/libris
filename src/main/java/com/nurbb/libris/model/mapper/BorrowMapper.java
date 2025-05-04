package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.response.BorrowResponse;
import com.nurbb.libris.model.entity.Borrow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BorrowMapper {

    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "createdAt", expression = "java(borrow.getCreatedDate().toString())")
    @Mapping(target = "updatedAt", expression = "java(borrow.getUpdatedDate().toString())")
    BorrowResponse toResponse(Borrow borrow);
}
