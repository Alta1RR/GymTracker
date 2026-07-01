package com.gymtracker.core.Service;

import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

@Service
public class ExerciseMediaService {
    private static final String DATASET_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/";
    private static final String DATASET_HOST = "raw.githubusercontent.com";
    private static final String DATASET_PATH_PREFIX = "/hasaneyldrm/exercises-dataset/main/";
    private static final String MEDIA_PATH_PATTERN = "^(images|videos)/[A-Za-z0-9._-]+\\.(jpg|jpeg|png|gif|webp)$";

    private final Path mediaRoot;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ExerciseMediaService(@Value("${exercise.media.local-root:/app/media}") String mediaRoot) {
        this.mediaRoot = Path.of(mediaRoot).toAbsolutePath().normalize();
    }

    public MediaResource load(String path, String url) {
        ResolvedMedia resolvedMedia = resolveSource(path, url);
        Path localPath = resolveLocalPath(resolvedMedia.path());
        if (Files.isRegularFile(localPath) && Files.isReadable(localPath)) {
            try {
                return new MediaResource(Files.readAllBytes(localPath), contentTypeFromPath(localPath.toString()));
            } catch (IOException e) {
                throw new RuntimeException("Cannot read local exercise media", e);
            }
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(resolvedMedia.sourceUri())
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Exercise media not found");
            }

            String contentType = response.headers()
                    .firstValue("content-type")
                    .orElse(contentTypeFromPath(resolvedMedia.path()));

            byte[] bytes = response.body();
            saveToLocalCache(localPath, bytes);
            return new MediaResource(bytes, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load exercise media", e);
        }
    }

    private ResolvedMedia resolveSource(String path, String url) {
        if (path != null && !path.isBlank()) {
            String cleanPath = normalizePath(path);
            validateMediaPath(cleanPath);
            return new ResolvedMedia(cleanPath, URI.create(DATASET_BASE_URL + cleanPath));
        }

        if (url == null || url.isBlank()) {
            throw new RuntimeException("Exercise media path is missing");
        }

        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !DATASET_HOST.equalsIgnoreCase(uri.getHost())
                || !uri.getPath().startsWith(DATASET_PATH_PREFIX)) {
            throw new RuntimeException("Exercise media source is not allowed");
        }

        String cleanPath = normalizePath(uri.getPath().substring(DATASET_PATH_PREFIX.length()));
        validateMediaPath(cleanPath);
        return new ResolvedMedia(cleanPath, URI.create(DATASET_BASE_URL + cleanPath));
    }

    private String normalizePath(String value) {
        return value.replace("\\", "/").replaceFirst("^/+", "");
    }

    private void validateMediaPath(String path) {
        if (!path.matches(MEDIA_PATH_PATTERN)) {
            throw new RuntimeException("Exercise media path is invalid");
        }
    }

    private Path resolveLocalPath(String path) {
        Path localPath = mediaRoot.resolve(path).normalize();
        if (!localPath.startsWith(mediaRoot)) {
            throw new RuntimeException("Exercise media path is invalid");
        }
        return localPath;
    }

    private void saveToLocalCache(Path localPath, byte[] bytes) {
        try {
            Files.createDirectories(localPath.getParent());
            Files.write(localPath, bytes);
        } catch (IOException ignored) {
            // Remote media is still returned even if the optional local cache cannot be written.
        }
    }

    private String contentTypeFromPath(String path) {
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if (lowerPath.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (lowerPath.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    public record MediaResource(byte[] bytes, String contentType) {
    }

    private record ResolvedMedia(String path, URI sourceUri) {
    }
}
