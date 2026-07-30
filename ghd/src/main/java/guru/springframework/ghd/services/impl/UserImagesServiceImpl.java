package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.user.UserImagesResponse;
import guru.springframework.ghd.entities.UserImage;
import guru.springframework.ghd.mappers.UserImagesMapper;
import guru.springframework.ghd.repositories.UserImagesRepository;
import guru.springframework.ghd.services.UserImagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserImagesServiceImpl implements UserImagesService {

    private final UserImagesRepository userImagesRepository;

    private final UserImagesMapper userImagesMapper;

    @Override
    public UserImagesResponse addUserImage(String userId, String url) {
        Integer lastOrder = userImagesRepository.findMaxOrderSetByUserId(userId);
        int nextOrder = (lastOrder == null) ? 1 : lastOrder + 1;

        UserImage newImage = UserImage.builder()
                .url(url)
                .userId(userId)
                .imageOrder(nextOrder)
                .build();
        return userImagesMapper.userImagesToUserImagesResponse(userImagesRepository.save(newImage));
    }
}
