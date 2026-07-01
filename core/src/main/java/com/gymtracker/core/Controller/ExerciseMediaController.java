package com.gymtracker.core.Controller;

import com.gymtracker.core.Service.ExerciseMediaService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class ExerciseMediaController {
    private final ExerciseMediaService exerciseMediaService;

    public ExerciseMediaController(ExerciseMediaService exerciseMediaService) {
        this.exerciseMediaService = exerciseMediaService;
    }

    @GetMapping("/exercise-media")
    public ResponseEntity<byte[]> getExerciseMedia(@RequestParam(value = "path", required = false) String path,
                                                   @RequestParam(value = "url", required = false) String url) {
        ExerciseMediaService.MediaResource media = exerciseMediaService.load(path, url);
        CacheControl cacheControl = "image/svg+xml".equals(media.contentType())
                ? CacheControl.noStore()
                : CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .cacheControl(cacheControl)
                .body(media.bytes());
    }
}
