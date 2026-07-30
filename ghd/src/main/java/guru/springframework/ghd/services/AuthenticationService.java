package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.auth.LoginRequest;
import guru.springframework.ghd.dto.user.UserResponse;

public interface AuthenticationService {
    UserResponse login(LoginRequest request);
}
