package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.user.PasswordRequest;
import guru.springframework.ghd.dto.user.UpdateProfileRequest;
import guru.springframework.ghd.dto.user.UserRequest;
import guru.springframework.ghd.dto.user.UserResponse;
import guru.springframework.ghd.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController extends BaseController {
    private final UserService userService;

    public static final String USER_PATH = "/api/v1/user";

    public static final String USER_PATH_ID = USER_PATH + "/{userId}";

    @GetMapping
    public ResponseEntity<?> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, defaultValue = DefaultPage.PAGE_STRING) Integer pageNumber,
            @RequestParam(required = false, defaultValue = DefaultPage.SIZE_STRING) Integer pageSize
    ) {
        List<UserResponse> pageUser = userService.getList(username, pageNumber, pageSize);

        return ok(pageUser);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse addUser(@Valid @ModelAttribute UserRequest user) {
        return userService.addUser(user);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getById(@PathVariable("userId") String userId) {
        Optional<UserResponse> pageUser = userService.getById(userId);

        return ok(pageUser);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal UserDetails currentUser) {
        // currentUser chứa thông tin username/email từ Token/Session
        String username = currentUser.getUsername();

        // Tìm thông tin đầy đủ từ database qua username
        UserResponse profile = userService.getByUsername(username);
        return ok(profile);
    }

    @PutMapping(value = USER_PATH_ID, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateById(@PathVariable("userId") String userId,  @Valid @ModelAttribute UserRequest user) {
        Optional<UserResponse> pageUser = userService.updateById(userId, user);

        return ok(pageUser);
    }

    @PutMapping(value = "/update")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Optional<UserResponse> updatedUser = userService.updateByUsername(currentUsername, user);

        return updatedUser.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/change-pw")
    public ResponseEntity<UserResponse> changePassword(
            Principal principal,
            @Valid @RequestBody PasswordRequest request) {
        System.out.println(principal.getName());

        return userService.updatePassword(principal.getName(), request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
