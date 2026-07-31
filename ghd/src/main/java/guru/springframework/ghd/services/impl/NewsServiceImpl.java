package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.DelFlag;
import guru.springframework.ghd.constants.enums.NewsStatus;
import guru.springframework.ghd.constants.enums.PostType;
import guru.springframework.ghd.dto.news.NewsProjection;
import guru.springframework.ghd.dto.news.NewsRequest;
import guru.springframework.ghd.dto.news.NewsResponse;
import guru.springframework.ghd.entities.News;
import guru.springframework.ghd.mappers.NewsMapper;
import guru.springframework.ghd.repositories.NewsRepository;
import guru.springframework.ghd.services.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.config.CacheConfig.NEWS;
import static guru.springframework.ghd.utils.PaginationUtil.getPageRequest;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    private final String SUB_FOLDER = "news";

    @Override
    @Transactional
    @CacheEvict(cacheNames = NEWS, allEntries = true)
    public NewsResponse addNews(NewsRequest request, MultipartFile image) {
        News news = newsMapper.toEntity(request);
        if (image != null && !image.isEmpty()) {
            String fileName = handleImageUpload(image, SUB_FOLDER);
            news.setThumbnail(fileName);
        } else {
            news.setThumbnail("default-thumbnail.png");
        }

        news.setCreatedDate(LocalDateTime.now());
        news.setViewCount(0);
        if (request.getPostType() != null) {
            news.setPostType(PostType.valueOf(request.getPostType()));
        } else {
            news.setPostType(PostType.NEWS);
        }

        News savedNews = newsRepository.save(news);

        return newsMapper.newsToNewsResponse(savedNews);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = NEWS, allEntries = true)
    public NewsResponse updateNews(String newsId, NewsRequest newsRequest, MultipartFile image) {
        News existingNews = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + newsId));
        if (newsRequest.getTitle() != null) {
            existingNews.setTitle(newsRequest.getTitle());
        }
        if (newsRequest.getSlug() != null) {
            existingNews.setSlug(newsRequest.getSlug());
        }
        if (newsRequest.getSummary() != null) {
            existingNews.setSummary(newsRequest.getSummary());
        }
        if (newsRequest.getContent() != null) {
            existingNews.setContent(newsRequest.getContent());
        }
        if (NewsStatus.valueOf(newsRequest.getStatus()) != null) {
            existingNews.setStatus(NewsStatus.valueOf(newsRequest.getStatus()));
        }
        if (newsRequest.getIsFeatured() != null) {
            existingNews.setIsFeatured(newsRequest.getIsFeatured());
        }
        if (newsRequest.getMetaDesc() != null) {
            existingNews.setMetaDesc(newsRequest.getMetaDesc());
        }
        if (newsRequest.getMetaTitle() != null) {
            existingNews.setMetaTitle(newsRequest.getMetaTitle());
        }
        if (newsRequest.getMetaKeyword() != null) {
            existingNews.setMetaKeyword(newsRequest.getMetaKeyword());
        }
        if (image != null && !image.isEmpty()) {
            String fileName = handleImageUpload(image, SUB_FOLDER);
            existingNews.setThumbnail(fileName);
        }
        if (newsRequest.getPostType() != null) {
            existingNews.setPostType(PostType.valueOf(newsRequest.getPostType()));
        }
        existingNews.setCategoryId(newsRequest.getCategoryId());
        existingNews.setBrandId(newsRequest.getBrandId());
        News updatedNews = newsRepository.save(existingNews);
        return newsMapper.newsToNewsResponse(updatedNews);
    }

    @Override
    public Page<NewsResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<NewsProjection> newsPage;

        if (StringUtils.hasText(title)) {
            newsPage = newsRepository.findAllByTitle(title, pageable);
        } else {
            newsPage = newsRepository.findAllNewsClient(pageable);
        }

        return newsPage.map(newsMapper::newsProjectionToNewsResponse);
    }

    @Override
    public Page<NewsResponse> getNewsAdmin(String title, String status, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<NewsProjection> newsPage;

        if (StringUtils.hasText(title) || StringUtils.hasText(status)) {
            newsPage = newsRepository.findNewsAdmin(title, status, pageable);
        } else {
            newsPage = newsRepository.findAllNewsAdmin(pageable);
        }

        return newsPage.map(newsMapper::newsProjectionToNewsResponse);
    }

    @Override
    public Optional<NewsResponse> getById(String id) {
        return newsRepository.findById(id).map(newsMapper::newsToNewsResponse);
    }

    @Override
    @Cacheable(cacheNames = NEWS, key = "'slug:' + #slug", unless = "#result == null")
    public NewsResponse getBySlug(String slug) {
        return newsRepository.findBySlug(slug).map(newsMapper::newsToNewsResponse).orElse(null);
    }

    @Override
    public void updateViewCount(UUID newsId) {
        // Check exist record
        News news = newsRepository.findById(newsId).orElse(null);
        if (news != null) {
            news.setViewCount(news.getViewCount() + 1);
            newsRepository.save(news);
        }
    }

    @Override
    @Cacheable(cacheNames = NEWS, key = "'featured'")
    public List<NewsResponse> getListFeatured() {
        List<NewsProjection> newsList = newsRepository.findNewsFeatured();

        return newsList.stream().map(newsMapper::newsProjectionToNewsResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = NEWS, key = "'related:' + #categoryTitle + ':' + #brandTitle", unless = "#result == null")
    public NewsResponse getRelatedNews(String categoryTitle, String brandTitle) {
        NewsProjection result;

        if (categoryTitle != null && brandTitle != null) {
            result = newsRepository.findByCategoryAndBrand(categoryTitle, brandTitle);
        } else if (categoryTitle != null) {
            result = newsRepository.findByCategory(categoryTitle);
        } else {
            result = newsRepository.findByBrand(brandTitle);
        }
        return newsMapper.newsProjectionToNewsResponse(result);
    }

    @Override
    @CacheEvict(cacheNames = NEWS, allEntries = true)
    public void deleteNewsById(String newsId) {
        News news = newsRepository.findById(newsId).orElse(null);
        if (news != null) {
            news.setDelFlag(DelFlag.NOT_ACTIVE.get());
            newsRepository.save(news);
        }
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize, String sortField, String sortDir) {
        return getPageRequest(pageNumber, pageSize, sortField, sortDir);
    }
}