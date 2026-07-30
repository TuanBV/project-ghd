package guru.springframework.ghd.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ImageService {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public String save(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File bị trống");
        }

        Path root = Paths.get(uploadPath);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + extension;

        Files.copy(file.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}