package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.slider.SliderResponse;
import guru.springframework.ghd.entities.Slider;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SliderMapper {
    Slider sliderResponseToSlider(SliderResponse sliderResponse);

    SliderResponse sliderToSliderResponse(Slider slider);
}
