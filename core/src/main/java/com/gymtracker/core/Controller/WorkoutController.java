package com.gymtracker.core.Controller;

import com.gymtracker.core.Repository.AchievementRepository;
import com.gymtracker.core.Repository.ExerciseRepository;
import com.gymtracker.core.Service.ExerciseMediaService;
import com.gymtracker.core.Service.WorkoutService;
import com.gymtracker.core.dto.ProgramCreateRequest;
import com.gymtracker.core.dto.ProgramUpdateRequest;
import com.gymtracker.core.dto.TemplateCreateRequest;
import com.gymtracker.core.dto.TemplateUpdateRequest;
import com.gymtracker.core.dto.WorkoutSaveRequest;
import com.gymtracker.core.dto.WorkoutSetDto;
import com.gymtracker.core.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gymtracker.core.dto.FriendProfileResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.gymtracker.core.config.TelegramAuthFilter.TELEGRAM_USER_ID_ATTRIBUTE;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final ExerciseMediaService exerciseMediaService;
    private final ExerciseRepository exerciseRepository;
    private final AchievementRepository achievementRepository;

    public WorkoutController(WorkoutService workoutService,
                             ExerciseMediaService exerciseMediaService,
                             ExerciseRepository exerciseRepository,
                             AchievementRepository achievementRepository) {
        this.workoutService = workoutService;
        this.exerciseMediaService = exerciseMediaService;
        this.exerciseRepository = exerciseRepository;
        this.achievementRepository = achievementRepository;
    }

    @PostMapping
    public ResponseEntity<String> saveWorkout(@RequestBody WorkoutSaveRequest workoutSaveRequest,
                                              HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, workoutSaveRequest.getTelegramId());
        workoutService.setWorkout(workoutSaveRequest);
        return ResponseEntity.ok("Workout saved successfully!");
    }

    @GetMapping("/exercises")
    public ResponseEntity<List<Exercise>> getAllExercises() {
        return ResponseEntity.ok(exerciseRepository.findAll());
    }

    @GetMapping("/media")
    public ResponseEntity<byte[]> getExerciseMedia(@RequestParam(value = "path", required = false) String path,
                                                   @RequestParam(value = "url", required = false) String url) {
        ExerciseMediaService.MediaResource media = exerciseMediaService.load(path, url);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(media.bytes());
    }

    @PostMapping("/programs")
    public ResponseEntity<Long> createProgram(@RequestBody ProgramCreateRequest programCreateRequest,
                                              HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, programCreateRequest.getTelegramId());
        Long programId = workoutService.createTrainingProgram(programCreateRequest);
        return ResponseEntity.ok(programId);
    }

    @PostMapping("/programs/{programId}/templates")
    public ResponseEntity<String> addTemplateToProgram(
            @PathVariable("programId") Long programId,
            @RequestParam("telegramId") Long telegramId,
            @RequestBody TemplateCreateRequest templateCreateRequest,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, telegramId);
        workoutService.addTemplateToProgram(programId, telegramId, templateCreateRequest);
        return ResponseEntity.ok("Template added to program successfully!");
    }

    @GetMapping("/programs")
    public ResponseEntity<List<TrainingProgram>> getMyPrograms(@RequestParam("telegramId") Long telegramId) {
        return ResponseEntity.ok(workoutService.getProgramsByUserId(telegramId));
    }

    @PutMapping("/programs/{id}")
    public ResponseEntity<TrainingProgram> updateProgram(
            @PathVariable("id") Long id,
            @RequestBody ProgramUpdateRequest programUpdateRequest,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, programUpdateRequest.getTelegramId());
        return ResponseEntity.ok(workoutService.updateProgram(id, programUpdateRequest));
    }

    @DeleteMapping("/programs/{id}")
    public ResponseEntity<String> deleteProgram(@PathVariable("id") Long id,
                                                @RequestParam("telegramId") Long telegramId,
                                                HttpServletRequest request){
        assertAuthenticatedTelegramId(request, telegramId);
        workoutService.deleteProgram(id, telegramId);
        return ResponseEntity.ok("Program deleted successfully!");
    }

    @DeleteMapping("/profile/reset")
    public ResponseEntity<String> resetProfile(@RequestParam("telegramId") Long telegramId,
                                               HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, telegramId);
        workoutService.resetUserProfile(telegramId);
        return ResponseEntity.ok("Profile reset successfully!");
    }

    @PostMapping("/programs/{id}/copy")
    public ResponseEntity<TrainingProgram> copySharedProgram(
            @PathVariable("id") Long id,
            @RequestParam("telegramId") Long telegramId,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, telegramId);
        return ResponseEntity.ok(workoutService.copySharedProgram(id, telegramId));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<WorkoutTemplate> updateTemplate(
            @PathVariable("id") Long id,
            @RequestBody TemplateUpdateRequest templateUpdateRequest,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, templateUpdateRequest.getTelegramId());
        return ResponseEntity.ok(workoutService.updateTemplate(id, templateUpdateRequest));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<String> deleteTemplate(
            @PathVariable("id") Long id,
            @RequestParam("telegramId") Long telegramId,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, telegramId);
        workoutService.deleteTemplate(id, telegramId);
        return ResponseEntity.ok("Template deleted successfully!");
    }

    @GetMapping("/latest")
    public ResponseEntity<List<WorkoutSetDto>> getLatestWorkout(
            @RequestParam("telegramId") Long telegramId,
            @RequestParam("name") String name) {
        List<WorkoutSetDto> latestSets = workoutService.getLatestWorkoutSets(telegramId, name);
        return ResponseEntity.ok(latestSets);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Workout>> getHistory(@RequestParam("telegramId") Long telegramId) {
        return ResponseEntity.ok(workoutService.getWorkoutHistory(telegramId));
    }

    @PostMapping("/friends/request")
    public ResponseEntity<String> sendFriendRequest(
            @RequestParam("requesterId") Long requesterId,
            @RequestParam("recipientId") Long recipientId,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, requesterId);
        workoutService.sendFriendRequest(requesterId, recipientId);
        return ResponseEntity.ok("Friend request sent successfully!");
    }

    @PostMapping("/friends/accept")
    public ResponseEntity<String> acceptFriendRequest(
            @RequestParam("requesterId") Long requesterId,
            @RequestParam("recipientId") Long recipientId,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, recipientId);
        workoutService.acceptFriendRequest(requesterId, recipientId);
        return ResponseEntity.ok("Friend request accepted successfully!");
    }

    @GetMapping("/friends")
    public ResponseEntity<List<AppUser>> getFriends(@RequestParam("telegramId") Long telegramId) {
        return ResponseEntity.ok(workoutService.getFriends(telegramId));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<UserAchievement>> getAchievements(@RequestParam("telegramId") Long telegramId) {
        return ResponseEntity.ok(workoutService.getEarnedAchievements(telegramId));
    }

    @GetMapping("/achievements/catalog")
    public ResponseEntity<List<Achievement>> getAchievementsCatalog() {
        return ResponseEntity.ok(achievementRepository.findAll());
    }

    @GetMapping("/friends/pending")
    public ResponseEntity<List<AppUser>> getPendingRequests(@RequestParam("telegramId") Long telegramId) {
        return ResponseEntity.ok(workoutService.getPendingRequests(telegramId));
    }

    // Получение готовых стандартных программ
    @GetMapping("/predefined")
    public ResponseEntity<List<TrainingProgram>> getPredefinedTemplates() {
        return ResponseEntity.ok(workoutService.getPredefinedPrograms());
    }

    // старт предустановленной программы - создаёт или возвращает существующую личную копию программы для этого пользователя, чтобы прогресс не был общим на всех
    @PostMapping("/predefined/{id}/start")
    public ResponseEntity<TrainingProgram> startPredefinedProgram(
            @PathVariable("id") Long id,
            @RequestParam("telegramId") Long telegramId,
            HttpServletRequest request) {
        assertAuthenticatedTelegramId(request, telegramId);
        TrainingProgram program = workoutService.startPredefinedProgram(telegramId, id);
        return ResponseEntity.ok(program);
    }

    @GetMapping("/friends/{friendTelegramId}/profile")
    public ResponseEntity<FriendProfileResponse> getFriendProfile(
            @RequestParam("telegramId") Long telegramId,
            @PathVariable("friendTelegramId") Long friendTelegramId) {
        return ResponseEntity.ok(workoutService.getFriendProfile(telegramId, friendTelegramId));
    }

    private void assertAuthenticatedTelegramId(HttpServletRequest request, Long telegramId) {
        Object authenticatedTelegramId = request.getAttribute(TELEGRAM_USER_ID_ATTRIBUTE);
        if (authenticatedTelegramId == null) {
            return;
        }

        if (!authenticatedTelegramId.equals(telegramId)) {
            throw new RuntimeException("Telegram id mismatch");
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
