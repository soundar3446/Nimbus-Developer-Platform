package com.nimbus.backend.user.mapper;

import com.nimbus.backend.user.dto.UserRequest;
import com.nimbus.backend.user.dto.UserResponse;
import com.nimbus.backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest request);

    UserResponse toResponse(User user);
}
