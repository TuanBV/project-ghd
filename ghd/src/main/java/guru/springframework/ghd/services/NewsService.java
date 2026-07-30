package guru.springframework.ghd.services;


import guru.springframework.ghd.dto.news.NewsRequest;
import guru.springframework.ghd.dto.news.NewsResponse;
import guru.springframework.ghd.entities.News;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface NewsService {
    NewsResponse addNews(NewsRequest news, MultipartFile image);

    NewsResponse updateNews(String newsId, NewsRequest news, MultipartFile image);

    Page<NewsResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Page<NewsResponse> getNewsAdmin(String title, String status, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Optional<NewsResponse> getById(String id);

    NewsResponse getBySlug(String slug);

    void updateViewCount(UUID id);

    List<NewsResponse> getListFeatured();

    NewsResponse getRelatedNews(String categoryTitle, String brandTitle);

    void deleteNewsById(String newsId);
}
