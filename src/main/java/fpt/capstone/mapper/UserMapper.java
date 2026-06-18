package fpt.capstone.mapper;

import fpt.capstone.dto.request.UserCreationRequest;
import fpt.capstone.dto.request.UserUpdateRequest;
import fpt.capstone.dto.response.UserResponse;
import fpt.capstone.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
    UserResponse toUserResponse(User user);
}
