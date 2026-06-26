package com.tcm.platform.dto;

/**
 * AI 养生问答响应。
 */
public record AIAnswerResponse(
        String answer,
        boolean fallback,
        String disclaimer
) {
}
