package iuh.fit.smartexambuilderbe.mappers;

import iuh.fit.smartexambuilderbe.dtos.requests.UserRegisterRequest;
import iuh.fit.smartexambuilderbe.dtos.responses.UserResponse;
import iuh.fit.smartexambuilderbe.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", source = "role")
    @Mapping(target = "status", source = "status")
    UserResponse toUserResponse(User user);

    @Mapping(target = "role", source = "role")
    User toUser(UserRegisterRequest user);
}
