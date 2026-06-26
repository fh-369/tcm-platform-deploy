package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件上传记录实体
 */
@Data
@TableName("uploads")
public class Upload {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalName;

    private String storedName;

    private String mimeType;

    private Long fileSize;

    private String accessUrl;

    private Long uploadedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
