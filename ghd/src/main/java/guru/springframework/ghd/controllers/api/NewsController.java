package guru.springframework.ghd.controllers.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.news.NewsRequest;
import guru.springframework.ghd.dto.news.NewsResponse;
import guru.springframework.ghd.services.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
public class NewsController extends BaseController {

    private final NewsService newsService;

    @GetMapping(value = "/list")
    public ResponseEntity<?> getNews(
            @RequestParam(name = "tu-khoa", required = false) String title,
            @RequestParam(name = "trang", required = false, defaultValue = DefaultPage.PAGE_STRING) int pageNumber,
            @RequestParam(name = "so-ban-ghi", required = false, defaultValue = DefaultPage.SIZE_CLIENT_STRING) int pageSize,
            @RequestParam(name = "thuoc-tinh", required = false, defaultValue = DefaultPage.CREATED_DATE) String sortField,
            @RequestParam(name = "kieu-sap-xep", required = false, defaultValue = DefaultPage.DESC) String sortDir
    ) {
        Page<NewsResponse> newsPage = newsService.getList(title, sortField, sortDir, pageNumber, pageSize);
        return ok(newsPage);
    }

    @GetMapping
    public ResponseEntity<?> getNewsAdmin(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = DefaultPage.PAGE_STRING) int page,
            @RequestParam(required = false, defaultValue = DefaultPage.SIZE_CLIENT_STRING) int size,
            @RequestParam(required = false, defaultValue = DefaultPage.CREATED_DATE) String field,
            @RequestParam(required = false, defaultValue = DefaultPage.DESC) String sort
    ) {
        Page<NewsResponse> newsPage = newsService.getNewsAdmin(title, status, field, sort, page, size);
        return ok(newsPage);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addNews(
            @RequestPart("data") @Valid NewsRequest newsRequest,
            @RequestPart(value = "image", required = false) Optional<MultipartFile> imageFile) {

        try {
            return ResponseEntity.ok(newsService.addNews(newsRequest, imageFile.orElse(null)));
        } catch (Exception e) {
            return ng(e.getMessage());
        }
    }

    @PutMapping(value = "/{newsId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateNews(
            @PathVariable("newsId") String newsId,
            @RequestPart("data") @Valid NewsRequest newsRequest,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        return ResponseEntity.ok(newsService.updateNews(newsId, newsRequest, imageFile));
    }

    @DeleteMapping(value = "/{newsId}")
    public ResponseEntity<?> deleteNews(@PathVariable("newsId") String newsId) {
        newsService.deleteNewsById(newsId);
        return ResponseEntity.ok(null);
    }
}
