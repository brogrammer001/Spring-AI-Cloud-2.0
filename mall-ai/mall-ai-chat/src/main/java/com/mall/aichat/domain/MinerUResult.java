package com.mall.aichat.domain;

import java.util.List;

public record MinerUResult(
    String markdown,
    List<ImageData> images
) {
    public record ImageData(String name, byte[] data, String mimeType) {}
}
