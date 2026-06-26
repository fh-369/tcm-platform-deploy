package com.tcm.platform.service;

import com.tcm.platform.dto.ContentImageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesSupportedImageWithGeneratedSafeName() throws Exception {
        ContentImageStorageService service = new ContentImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "春季 封面.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        ContentImageResponse result = service.store(file);

        assertThat(result.url()).startsWith("/uploads/content/").endsWith(".png");
        assertThat(result.originalName()).isEqualTo("春季 封面.png");
        assertThat(Files.list(tempDir.resolve("content"))).hasSize(1);
    }

    @Test
    void rejectsEmptyFile() {
        ContentImageStorageService service = new ContentImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择需要上传的图片");
    }

    @Test
    void rejectsUnsupportedContentType() {
        ContentImageStorageService service = new ContentImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.gif",
                "image/gif",
                new byte[]{1}
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("封面仅支持 JPG、PNG 或 WebP 格式");
    }

    @Test
    void rejectsFilesLargerThanFiveMegabytes() {
        ContentImageStorageService service = new ContentImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("封面图片不能超过 5 MB");
    }
}
