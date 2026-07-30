package guru.springframework.ghd.services;


import guru.springframework.ghd.dto.user.UserImagesResponse;

public interface UserImagesService {
    UserImagesResponse addUserImage(String userId, String url);
}
