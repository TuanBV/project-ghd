package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.services.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/image")
public class ImageController extends BaseController {
    private final ImageService imageService;

    private final String SUB_FOLDER = "products";

    @PostMapping("/upload")
    public Map<String, Object> uploadImage(@RequestParam Map<String, MultipartFile> fileMap) {
        Map<String, Object> response = new HashMap<>();

        if (fileMap == null || fileMap.isEmpty()) {
            response.put("errorMessage", "Không có file upload");
            return response;
        }

        try {
            List<Map<String, Object>> result = new ArrayList<>();

            for (MultipartFile file : fileMap.values()) {
                if (file.isEmpty()) continue;

                // Save image
                String baseFileUrl = handleImageUpload(file, SUB_FOLDER);

                if (baseFileUrl != null) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("url", baseFileUrl + "_pc.webp");
                    fileInfo.put("base_url", baseFileUrl);
                    fileInfo.put("mobile", baseFileUrl + "_mobile.webp");
                    fileInfo.put("tablet", baseFileUrl + "_tablet.webp");
                    fileInfo.put("pc", baseFileUrl + "_pc.webp");
                    fileInfo.put("name", file.getOriginalFilename());
                    result.add(fileInfo);
                }
            }

            response.put("result", result);
            return response;
        } catch (Exception e) {
            response.put("errorMessage", "Upload thất bại: " + e.getMessage());
            return response;
        }
    }
}
