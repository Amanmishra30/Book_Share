package com.bookshare.service;

import java.util.List;

/**
 * Hook for the OPTIONAL YOLO-based damage detection (Phase 2 of the condition check).
 * It is NOT implemented in this project. To add it, create a Spring bean implementing this interface
 * (for example one that calls a Python/ONNX inference service). When a bean exists, its note is stored in
 * ConditionCheck.aiNotes for pickup/return checks and shown to admins; when none exists, nothing happens.
 */
public interface DamageDetector {
    /** @return a short note such as "possible water damage on spine", or null when nothing was flagged. */
    String analyse(List<String> photoUrls);
}
