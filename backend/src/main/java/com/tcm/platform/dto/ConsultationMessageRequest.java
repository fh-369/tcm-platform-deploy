package com.tcm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationMessageRequest {

    @NotBlank(message = "请输入回复内容")
    @Size(max = 2000, message = "回复内容不能超过 2000 字")
    private String content;
}

