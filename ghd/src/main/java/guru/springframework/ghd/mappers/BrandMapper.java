package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.brand.BrandResponse;
import guru.springframework.ghd.entities.Brand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    Brand brandResponseToBrand(BrandResponse brandResponse);

    BrandResponse brandToBrandResponse(Brand brand);
}
