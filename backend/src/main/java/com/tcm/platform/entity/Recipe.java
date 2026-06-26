package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 药膳实体
 */
@Data
@TableName("recipes")
public class Recipe {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String season;

    private String constitution;

    private String suitableFor;

    private String summary;

    private String ingredients;

    private String steps;

    private String imageUrl;

    private Boolean published;

    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
