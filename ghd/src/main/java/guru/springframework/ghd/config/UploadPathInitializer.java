package guru.springframework.ghd.config;

import guru.springframework.ghd.utils.UploadImageUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wires the {@code upload.path} configuration property into {@link UploadImageUtil},
 * which is a static utility shared across service implementations and cannot be
 * injected with {@code @Value} directly.
 */
@Component
public class UploadPathInitializer {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        UploadImageUtil.init(uploadPath);
    }
}
