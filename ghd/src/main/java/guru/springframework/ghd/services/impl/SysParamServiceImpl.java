package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.constants.enums.DelFlag;
import guru.springframework.ghd.dto.sysparam.SysParamRequest;
import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import guru.springframework.ghd.entities.SysParam;
import guru.springframework.ghd.mappers.SysParamMapper;
import guru.springframework.ghd.repositories.SysParamRepository;
import guru.springframework.ghd.services.SysParamService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static guru.springframework.ghd.config.CacheConfig.SYS_PARAMS;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysParamServiceImpl implements SysParamService {

    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SysParamRepository sysParamRepository;
    private final SysParamMapper sysParamMapper;

    @Value("${seo.static.path:seo}")
    private String seoStaticPath;

    @Override
    public List<SysParamResponse> getList(String groupCode) {
        return List.of();
    }

    @Override
    @Cacheable(cacheNames = SYS_PARAMS, key = "'all'")
    public List<SysParamResponse> getAll() {
        // Collectors.toList() (not Stream.toList()) so the cached value is a plain
        // ArrayList: Stream.toList()'s immutable JDK-internal list class is `final`,
        // which Spring's Redis Jackson serializer deliberately never wraps with type
        // info, so a non-empty result fails to deserialize back out of the cache.
        return sysParamRepository.findAll()
                .stream()
                .filter(item -> Objects.equals(item.getDelFlag(), DelFlag.ACTIVE.get()))
                .map(sysParamMapper::sysParamToSysParamResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SysParamResponse> getById(Long id) {
        return sysParamRepository.findById(id).map(this::mapToResponse);
    }

    @Override
    // Spring unwraps Optional<T> return values before evaluating "unless" (and before
    // caching), so #result here is the unwrapped SysParamResponse (or null) - not the
    // Optional itself.
    @Cacheable(cacheNames = SYS_PARAMS, key = "'key:' + #key", unless = "#result == null")
    public Optional<SysParamResponse> getByKey(String key) {
        return sysParamRepository.findByParamKey(key).map(this::mapToResponse);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = SYS_PARAMS, allEntries = true)
    public void updateSysParam(List<SysParamRequest> sysParamRequests) {
        List<SysParam> updatedList = new ArrayList<>();
        for (SysParamRequest req : sysParamRequests) {
            sysParamRepository.findByParamKey(req.getParamKey()).ifPresent(param -> {
                param.setParamValue(req.getParamValue());
                updatedList.add(param);
            });
        }

        sysParamRepository.saveAll(updatedList);
    }

    @Override
    public void uploadSitemap(MultipartFile file) throws IOException {
        uploadSeoFile("sitemap", file);
    }

    @Override
    public void uploadSeoFile(String type, MultipartFile file) throws IOException {
        String fileName = resolveSeoFileName(type);
        Path targetDirectory = Paths.get(seoStaticPath).toAbsolutePath().normalize();
        Path targetPath = targetDirectory.resolve(fileName).normalize();

        if (!targetPath.startsWith(targetDirectory)) {
            throw new IOException("Invalid SEO file path");
        }

        Files.createDirectories(targetDirectory);
        validateUploadedFile(file, type);
        backupExistingFile(targetDirectory, targetPath, fileName);
        replaceFileAtomically(file, targetDirectory, targetPath, fileName);
    }

    private String resolveSeoFileName(String type) {
        if ("sitemap".equalsIgnoreCase(type)) {
            return "sitemap.xml";
        }
        if ("robots".equalsIgnoreCase(type)) {
            return "robots.txt";
        }
        throw new IllegalArgumentException("Unsupported SEO file type: " + type);
    }

    private void validateUploadedFile(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalName = Objects.toString(file.getOriginalFilename(), "").toLowerCase();
        if ("sitemap".equalsIgnoreCase(type) && !originalName.endsWith(".xml")) {
            throw new IllegalArgumentException("Sitemap upload must be an .xml file");
        }
        if ("robots".equalsIgnoreCase(type) && !originalName.endsWith(".txt")) {
            throw new IllegalArgumentException("Robots upload must be a .txt file");
        }
    }

    private void backupExistingFile(Path targetDirectory, Path targetPath, String fileName) throws IOException {
        if (!Files.exists(targetPath)) {
            return;
        }

        Path backupDirectory = targetDirectory.resolve("backup");
        Files.createDirectories(backupDirectory);

        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMATTER);
        Path backupPath = backupDirectory.resolve(baseName + "_" + timestamp + extension);

        Files.copy(targetPath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void replaceFileAtomically(MultipartFile file, Path targetDirectory, Path targetPath, String fileName)
            throws IOException {
        Path tempPath = Files.createTempFile(targetDirectory, fileName + "-", ".tmp");
        try {
            file.transferTo(tempPath);
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private SysParamResponse mapToResponse(SysParam entity) {
        return SysParamResponse.builder()
                .paramKey(entity.getParamKey())
                .paramValue(entity.getParamValue())
                .paramName(entity.getParamName())
                .groupCode(entity.getGroupCode())
                .description(entity.getDescription())
                .build();
    }
}
