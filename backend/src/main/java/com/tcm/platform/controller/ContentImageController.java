package com.tcm.platform.controller;

import com.tcm.platform.common.Result;
import com.tcm.platform.dto.ContentImageResponse;
import com.tcm.platform.service.ContentImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/content-images")
public class ContentImageController {

    private final ContentImageStorageService contentImageStorageService;

    public ContentImageController(ContentImageStorageService contentImageStorageService) {
        this.contentImageStorageService = contentImageStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ContentImageResponse> upload(@RequestParam("file") MultipartFile file) {
        return Result.success("封面上传成功", contentImageStorageService.store(file));
    }
}
