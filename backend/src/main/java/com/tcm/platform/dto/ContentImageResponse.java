package com.tcm.platform.dto;

public record ContentImageResponse(
        String url,
        String originalName,
        long size
) {
}
