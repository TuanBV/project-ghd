package guru.springframework.ghd.validations;

import guru.springframework.ghd.dto.news.NewsRequest;
import guru.springframework.ghd.entities.News;
import guru.springframework.ghd.repositories.NewsRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class UniqueSlugValidator implements ConstraintValidator<UniqueSlug, NewsRequest> {

    @Autowired
    private NewsRepository newsRepository;

    @Override
    public boolean isValid(NewsRequest dto, ConstraintValidatorContext context) {
        if (dto.getSlug() == null || dto.getSlug().isEmpty()) {
            return true;
        }

        Optional<News> existingNews = newsRepository.findBySlug(dto.getSlug());

        if (existingNews.isEmpty()) {
            return true;
        }

        if (dto.getId() != null && dto.getId().equals(existingNews.get().getId().toString())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("slug")
                .addConstraintViolation();

        return false;
    }
}