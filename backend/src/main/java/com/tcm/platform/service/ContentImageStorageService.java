package com.tcm.platform.service;

import com.tcm.platform.dto.ContentImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ContentImageStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path contentDirectory;

    public ContentImageStorageService(@Value("${tcm.upload.path:./uploads}") String uploadPath) {
        this.contentDirectory = Path.of(uploadPath).toAbsolutePath().normalize().resolve("content");
    }

    public ContentImageResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("封面图片不能超过 5 MB");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("封面仅支持 JPG、PNG 或 WebP 格式");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = contentDirectory.resolve(storedName).normalize();
        if (!target.startsWith(contentDirectory)) {
            throw new IllegalArgumentException("封面文件名无效");
        }

        try {
            Files.createDirectories(contentDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("封面图片保存失败", exception);
        }

        return new ContentImageResponse(
                "/uploads/content/" + storedName,
                file.getOriginalFilename(),
                file.getSize()
        );
    }
}
