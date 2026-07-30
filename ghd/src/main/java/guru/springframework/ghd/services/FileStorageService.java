package guru.springframework.ghd.services;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String save(MultipartFile file);
    void delete(String fileUrl);
    String saveWithSubFolder(MultipartFile file, String folder);
}
