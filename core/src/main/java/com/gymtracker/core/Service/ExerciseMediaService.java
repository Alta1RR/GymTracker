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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExerciseMediaService {
    private static final String DATASET_MEDIA_BASE_URL = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/6bf7b87/";
    private static final String DATASET_HOST = "raw.githubusercontent.com";
    private static final String DATASET_PATH_PREFIX = "/hasaneyldrm/exercises-dataset/main/";
    private static final String MEDIA_PATH_PATTERN = "^(images|videos)/[A-Za-z0-9._-]+\\.(jpg|jpeg|png|gif|webp)$";
    private static final Pattern DATASET_RAW_PATH_PATTERN = Pattern.compile("^/hasaneyldrm/exercises-dataset/[^/]+/(images|videos)/(.+)$");

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
            return fallbackSvg();
        }
    }

    private ResolvedMedia resolveSource(String path, String url) {
        if (path != null && !path.isBlank()) {
            String cleanPath = normalizePath(path);
            validateMediaPath(cleanPath);
            return new ResolvedMedia(cleanPath, URI.create(DATASET_MEDIA_BASE_URL + cleanPath));
        }

        if (url == null || url.isBlank()) {
            throw new RuntimeException("Exercise media path is missing");
        }

        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new RuntimeException("Exercise media source is not allowed");
        }

        if (DATASET_HOST.equalsIgnoreCase(uri.getHost()) && uri.getPath().startsWith(DATASET_PATH_PREFIX)) {
            String cleanPath = normalizePath(uri.getPath().substring(DATASET_PATH_PREFIX.length()));
            validateMediaPath(cleanPath);
            return new ResolvedMedia(cleanPath, URI.create(DATASET_MEDIA_BASE_URL + cleanPath));
        }

        if (DATASET_HOST.equalsIgnoreCase(uri.getHost())) {
            Matcher matcher = DATASET_RAW_PATH_PATTERN.matcher(uri.getPath());
            if (matcher.matches()) {
                String cleanPath = normalizePath(matcher.group(1) + "/" + matcher.group(2));
                validateMediaPath(cleanPath);
                return new ResolvedMedia(cleanPath, URI.create(DATASET_MEDIA_BASE_URL + cleanPath));
            }
        }

        throw new RuntimeException("Exercise media source is not allowed");
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

    private MediaResource fallbackSvg() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160">
                  <rect width="160" height="160" rx="24" fill="#111827"/>
                  <circle cx="80" cy="62" r="24" fill="#1d4ed8" opacity=".22"/>
                  <path d="M42 94h76M52 94V74M108 94V74M64 74h32" stroke="#60a5fa" stroke-width="8" stroke-linecap="round"/>
                  <text x="80" y="124" text-anchor="middle" fill="#94a3b8" font-family="Arial, sans-serif" font-size="14" font-weight="700">GYMTRACKER</text>
                </svg>
                """;
        return new MediaResource(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
    }

    public record MediaResource(byte[] bytes, String contentType) {
    }

    private record ResolvedMedia(String path, URI sourceUri) {
    }
}
