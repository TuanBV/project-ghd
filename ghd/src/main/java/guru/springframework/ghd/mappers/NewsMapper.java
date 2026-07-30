package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.news.NewsProjection;
import guru.springframework.ghd.dto.news.NewsRequest;
import guru.springframework.ghd.dto.news.NewsResponse;
import guru.springframework.ghd.entities.News;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NewsMapper {
    News newsResponseToNews(NewsResponse newsResponse);

    NewsResponse newsToNewsResponse(News news);

    NewsResponse newsProjectionToNewsResponse(NewsProjection newsProjection);

    News toEntity(NewsRequest request);
}