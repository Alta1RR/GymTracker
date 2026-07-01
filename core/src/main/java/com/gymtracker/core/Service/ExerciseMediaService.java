package com.gymtracker.core.Service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Service
public class ExerciseMediaService {
    private static final String DATASET_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/";
    private static final String DATASET_HOST = "raw.githubusercontent.com";
    private static final String DATASET_PATH_PREFIX = "/hasaneyldrm/exercises-dataset/main/";
    private static final String MEDIA_PATH_PATTERN = "^(images|videos)/[A-Za-z0-9._-]+\\.(jpg|jpeg|png|gif|webp)$";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public MediaResource load(String path, String url) {
        URI sourceUri = resolveSourceUri(path, url);

        try {
            HttpRequest request = HttpRequest.newBuilder(sourceUri)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Exercise media not found");
            }

            String contentType = response.headers()
                    .firstValue("content-type")
                    .orElse(contentTypeFromPath(sourceUri.getPath()));

            return new MediaResource(response.body(), contentType);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load exercise media", e);
        }
    }

    private URI resolveSourceUri(String path, String url) {
        if (path != null && !path.isBlank()) {
            String cleanPath = normalizePath(path);
            validateMediaPath(cleanPath);
            return URI.create(DATASET_BASE_URL + cleanPath);
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
        return URI.create(DATASET_BASE_URL + cleanPath);
    }

    private String normalizePath(String value) {
        return value.replace("\\", "/").replaceFirst("^/+", "");
    }

    private void validateMediaPath(String path) {
        if (!path.matches(MEDIA_PATH_PATTERN)) {
            throw new RuntimeException("Exercise media path is invalid");
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
}
