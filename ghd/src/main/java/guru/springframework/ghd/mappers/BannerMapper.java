package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.banner.BannerResponse;
import guru.springframework.ghd.entities.Banner;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BannerMapper {

    BannerResponse bannerToBannerResponse(Banner banner);

    Banner bannerResponseToBanner(BannerResponse bannerResponse);
}