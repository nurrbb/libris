package com.nurbb.libris.model.mapper;

import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.dto.response.UserResponse;
import com.nurbb.libris.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class})
public interface UserMapper {


    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    User toEntity(UserRequest request);

    @Mapping(target = "createdAt", expression = "java(user.getCreatedDate().toString())")
    @Mapping(target = "updatedAt", expression = "java(user.getUpdatedDate().toString())")
    UserResponse toResponse(User user);
}