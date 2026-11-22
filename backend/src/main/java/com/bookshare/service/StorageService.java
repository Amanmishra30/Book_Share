package com.bookshare.service;

import com.bookshare.web.ApiException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public interface StorageService {
    /** Stores one image and returns the URL the browser can load it from. */
    String store(MultipartFile file);

    Set<String> EXT = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    static String extensionOf(MultipartFile f) {
        String n = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase(Locale.ROOT);
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i);
    }

    static void requireImage(MultipartFile f) {
        String type = f.getContentType() == null ? "" : f.getContentType();
        if (f.isEmpty() || !type.startsWith("image/") || !EXT.contains(extensionOf(f))) {
            throw ApiException.badRequest("Only JPG, PNG, WEBP or GIF images are accepted.");
        }
    }
}
