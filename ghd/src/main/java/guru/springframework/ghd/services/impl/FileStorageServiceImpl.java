package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.services.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${upload.path:uploads}")
    private String uploadPath;

    private Path root;

    @PostConstruct
    public void init() {
        try {
            root = Paths.get(uploadPath).toAbsolutePath();
            // If root don't exist then create uploads folder
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("Don't init root directory!");
        }
    }

    @Override
    public String save(MultipartFile file) {
        // Don't sub_folder then save image to uploads/
        return saveWithSubFolder(file, "");
    }

    /**
     * Save file to sub_folder in "uploads"
     * @param subFolder Ex: "products", "brands"
     */
    public String saveWithSubFolder(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            // Call handle image (resize + webp + async)
            return handleImageUpload(file, subFolder);
        } catch (Exception e) {
            throw new RuntimeException("Error image upload: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String pathToFile) {
        try {
            if (pathToFile == null || pathToFile.isEmpty()) return;

            String searchString = "uploads/";
            int index = pathToFile.indexOf(searchString);

            String relativeToUploads;
            if (index != -1) {
                relativeToUploads = pathToFile.substring(index + searchString.length());
            } else {
                relativeToUploads = pathToFile.startsWith("/") ? pathToFile.substring(1) : pathToFile;
            }

            Path file = root.resolve(relativeToUploads).normalize();

            if (!file.startsWith(root)) {
                System.err.println("Warning: Delete file outside uploads folder!");
                return;
            }

            if (Files.deleteIfExists(file)) {
                System.out.println("Delete file image: " + file);
            }

        } catch (IOException e) {
            System.err.println("Error delete file: " + e.getMessage());
        }
    }
}