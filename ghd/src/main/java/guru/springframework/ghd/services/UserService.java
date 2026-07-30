package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.user.PasswordRequest;
import guru.springframework.ghd.dto.user.UpdateProfileRequest;
import guru.springframework.ghd.dto.user.UserRequest;
import guru.springframework.ghd.dto.user.UserResponse;
import jakarta.validation.Valid;

import java.util.*;

public interface UserService {
    List<UserResponse> getList(String username, Integer pageNumber, Integer pageSize);

    Optional<UserResponse> getById(String userId);

    Optional<UserResponse> updateById(String userId, UserRequest user);

    void deleteById(UUID userId);

    UserResponse addUser(UserRequest user);

    UserResponse getByUsername(String username);

    Optional<UserResponse> updateByUsername(String username, UpdateProfileRequest user);

    Optional<UserResponse> updatePassword(String currentUsername, @Valid PasswordRequest user);
}
