package com.tcm.platform.dto;

/**
 * AI 问答结束后展示的站内延伸阅读。
 */
public record AIContentRecommendation(
        Long id,
        String type,
        String title,
        String description
) {
}
