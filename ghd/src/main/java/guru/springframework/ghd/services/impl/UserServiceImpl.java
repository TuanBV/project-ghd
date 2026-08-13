package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.user.PasswordRequest;
import guru.springframework.ghd.dto.user.UpdateProfileRequest;
import guru.springframework.ghd.dto.user.UserRequest;
import guru.springframework.ghd.dto.user.UserResponse;
import guru.springframework.ghd.entities.User;
import guru.springframework.ghd.entities.UserImage;
import guru.springframework.ghd.events.UserRegisteredEvent;
import guru.springframework.ghd.mappers.UserMapper;
import guru.springframework.ghd.repositories.UserImagesRepository;
import guru.springframework.ghd.repositories.UserRepository;
import guru.springframework.ghd.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static guru.springframework.ghd.config.KafkaTopicConfig.USER_EVENTS_TOPIC;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final UserRepository userRepository;

    private final UserImagesRepository userImagesRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private  final UserMapper userMapper;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final String SUB_FOLDER = "users/";

    private final String PASSWORD_DEFAULT = "Test123@";

    @Override
    public List<UserResponse> getList(String username, Integer pageNumber, Integer pageSize) {

        Page<User> userPage;

        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize);

        if (StringUtils.hasText(username)) {
            userPage = listUsername(username);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }

        return userPage.map(userMapper::userToUserResponse).stream().toList();
    }

    private Page<User> listUsername(String username) {
        return userRepository.findAllByUsernameIsLikeIgnoreCase("%" + username + "%", null);
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize) {
        int queryPageNumber;
        int queryPageSize;

        if (pageNumber != null && pageNumber > 0) {
            queryPageNumber = pageNumber - 1;
        } else {
            queryPageNumber = DEFAULT_PAGE;
        }
        if (pageSize == null) {
            queryPageSize = DEFAULT_PAGE_SIZE;
        } else {
            queryPageSize = pageSize;
        }

        Sort sort = Sort.by(Sort.Order.desc("username"));

        return PageRequest.of(queryPageNumber, queryPageSize, sort);
    }

    @Override
    public Optional<UserResponse> getById(String userId) {
        return userRepository.findById(userId).map(userMapper::userToUserResponse);
    }

    @Override
    public Optional<UserResponse> updateById(String userId, UserRequest user) {
        // Check exists user
        User exists = userRepository.findById(userId).orElse(null);
        if (exists == null) {
            throw new RuntimeException("User not found");
        }

        // Set value
        exists.setUsername(user.getUsername());
        exists.setEmail(user.getEmail());
        if (!user.getPhone().isEmpty()) {
            exists.setPhone(user.getPhone());

        }
        return Optional.of(userMapper.userToUserResponse(userRepository.save(exists)));
    }

    @Override
    public void deleteById(UUID userId) {

    }

    @Override
    public UserResponse addUser(UserRequest user) {
        String rawPassword = (user.getPassword() == null || user.getPassword().trim().isEmpty())
                ? PASSWORD_DEFAULT
                : user.getPassword();

        // Encoded password
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Save new user
        User newUser = User.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .phone(user.getPhone())
                .password(encodedPassword)
                .build();
        User savedUser = userRepository.save(newUser);

        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            String fileName = handleImageUpload(user.getAvatar(), SUB_FOLDER);

            UserImage userImage = UserImage.builder()
                    .url(fileName)
                    .userId(savedUser.getId().toString())
                    .imageOrder(1)
                    .build();
            userImagesRepository.save(userImage).getUrl();
        }

        try {
            kafkaTemplate.send(USER_EVENTS_TOPIC, savedUser.getId().toString(),
                    new UserRegisteredEvent(savedUser.getId().toString(), savedUser.getUsername(), savedUser.getEmail()));
        } catch (Exception e) {
            log.warn("Không publish được sự kiện user.registered cho user {}: {}", savedUser.getId(), e.getMessage());
        }

        return userMapper.userToUserResponse(savedUser);
    }

    @Override
    public UserResponse getByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::userToUserResponse).orElse(null);
    }
    @Transactional
    @Override
    public Optional<UserResponse> updateByUsername(String username, UpdateProfileRequest request) {
        return userRepository.findByUsername(username).map(user -> {
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            User updated = userRepository.save(user);
            return userMapper.userToUserResponse(updated);
        });
    }

    @Transactional
    @Override
    public Optional<UserResponse> updatePassword(String currentUsername, PasswordRequest request) {
        return userRepository.findByUsername(currentUsername).map(user -> {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Mật khẩu cũ không chính xác");
            }
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                throw new RuntimeException("Mật khẩu mới không được giống mật khẩu hiện tại");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            User updatedUser = userRepository.save(user);

            return userMapper.userToUserResponse(updatedUser);
        });
    }
}
