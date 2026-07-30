package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.user.UserImagesResponse;
import guru.springframework.ghd.entities.UserImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserImagesMapper {
    UserImage userImagesResponseToUserImages(UserImagesResponse userImagesResponse);

    UserImagesResponse userImagesToUserImagesResponse(UserImage userImage);
}
