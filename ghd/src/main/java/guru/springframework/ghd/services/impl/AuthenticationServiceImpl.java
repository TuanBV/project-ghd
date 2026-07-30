package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.auth.LoginRequest;
import guru.springframework.ghd.dto.user.UserResponse;
import guru.springframework.ghd.entities.User;
import guru.springframework.ghd.repositories.AuthenticationRepository;
import guru.springframework.ghd.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationRepository authenticationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse login(LoginRequest request) {
        // Get user by username
        Optional<User> authentication = authenticationRepository.findByUsername(request.getUsername());

        // Check exists
        if (authentication.isEmpty()) {
            throw new UsernameNotFoundException("Username not found");
        }

        if (!passwordEncoder.matches(request.getPassword(), authentication.get().getPassword())) {
            throw new BadCredentialsException("Username hoặc mật khẩu không chính xác");
        }

        return UserResponse.builder()
                .id(authentication.get().getId())
                .username(authentication.get().getUsername())
                .password(authentication.get().getPassword())
                .email(authentication.get().getEmail())
                .phone(authentication.get().getPhone())
                .delFlag(authentication.get().getDelFlag())
                .build();
    }
}
