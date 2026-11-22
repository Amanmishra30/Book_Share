package com.bookshare.service;

import com.bookshare.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private final Path dir;

    public LocalStorageService(@Value("${app.storage.local-dir}") String dir) throws IOException {
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.dir);
    }

    @Override
    public String store(MultipartFile file) {
        StorageService.requireImage(file);
        String name = UUID.randomUUID() + StorageService.extensionOf(file);
        try {
            Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the photo.");
        }
        return "/uploads/" + name;
    }
}
