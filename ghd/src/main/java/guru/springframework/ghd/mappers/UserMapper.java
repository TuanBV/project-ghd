package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.user.UserResponse;
import guru.springframework.ghd.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User userResponseToUser(UserResponse userResponse);

    UserResponse userToUserResponse(User user);
}
