package guru.springframework.ghd.utils;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class UploadImageUtil {

    // Root directory for uploads, initialized from the `upload.path` configuration property
    // (see UploadPathInitializer). Defaults to "uploads" so this keeps working outside Spring
    // (e.g. plain unit tests) or before the context has started.
    private static volatile String rootDir = "uploads";

    public static void init(String uploadPath) {
        rootDir = StringUtils.hasText(uploadPath) ? uploadPath : "uploads";
    }

    /**
     * Handles image processing, resizing, and saving in WebP format.
     * Uses the original filename with a timestamp to ensure uniqueness.
     */
    public static String handleImageUpload(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) return null;

        try {
            // 1. Get and clean the base filename
            String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String baseName = getBaseNameWithoutExtension(originalFileName);

            // 2. Prepare the directory path
            Path uploadPath = Paths.get(rootDir, subFolder).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // --- START DUPLICATE FILENAME HANDLING ---
            String finalBaseName = baseName;
            int count = 1;

            // Check if a file with this name already exists (checking the mobile version as a proxy)
            // If it exists, append a counter: "image" -> "image-1", "image-2", etc.
            while (Files.exists(uploadPath.resolve(finalBaseName + "_mobile.webp"))) {
                finalBaseName = baseName + "-" + count;
                count++;
            }
            // --- END DUPLICATE FILENAME HANDLING ---

            // 3. Load the image from the input stream
            ImmutableImage originalImage = ImmutableImage.loader().fromStream(file.getInputStream());

            // 4. Save optimized responsive versions in WebP format using the unique finalBaseName
            saveAsWebP(originalImage.scaleToWidth(400), uploadPath, finalBaseName + "_mobile");
            saveAsWebP(originalImage.scaleToWidth(800), uploadPath, finalBaseName + "_tablet");
            saveAsWebP(originalImage.scaleToWidth(1200), uploadPath, finalBaseName + "_pc");

            // 5. Return the relative path/prefix for Database storage
            return "/uploads/" + subFolder + "/" + finalBaseName;

        } catch (Exception e) {
            System.err.println("FAILED to upload image: " + e.getMessage());
            throw new RuntimeException("Error during image processing: " + e.getMessage(), e);
        }
    }

    private static void saveAsWebP(ImmutableImage image, Path uploadPath, String fileName) {
        try {
            Path filePath = uploadPath.resolve(fileName + ".webp");
            // Set WebP quality to 70% if not needed for better compression
            WebpWriter writer = WebpWriter.DEFAULT.withQ(70).withM(4);
            image.output(writer, filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing WebP file " + fileName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Removes the file extension and sanitizes the filename.
     * Example: "Summer Vacation 2024.jpg" -> "summer-vacation-2024"
     */
    private static String getBaseNameWithoutExtension(String fileName) {
        if (fileName == null) return "image";

        int lastDotIndex = fileName.lastIndexOf(".");
        String name = (lastDotIndex == -1) ? fileName : fileName.substring(0, lastDotIndex);

        // 1. Normalize: Convert "ả" to "a" + accent mark
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);

        // 2. Remove all accent marks (Diacritics)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String latinName = pattern.matcher(normalized).replaceAll("");

        // 3. Replace 'đ' and 'Đ' manually (Normalizer doesn't handle these)
        latinName = latinName.replace("đ", "d").replace("Đ", "D");

        // 4. Final Cleanup: replace spaces/specials with hyphens, lowercase it
        return latinName.replaceAll("[^a-zA-Z0-9.-]", "-")
                .replaceAll("-+", "-") // Remove double hyphens like "--"
                .toLowerCase();
    }
}