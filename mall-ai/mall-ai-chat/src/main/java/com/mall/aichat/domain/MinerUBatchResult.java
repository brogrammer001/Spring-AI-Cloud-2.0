package com.mall.aichat.domain;

import java.util.List;

public record MinerUBatchResult(int code, String msg, Data data) {
    public record Data(String batch_id, List<ExtractResult> extract_result) {
    }

    public record ExtractResult(String file_name, String state, String err_msg,
                                String full_zip_url, ExtractProgress extract_progress) {
    }

    public record ExtractProgress(int extracted_pages, int total_pages, String start_time) {
    }
}