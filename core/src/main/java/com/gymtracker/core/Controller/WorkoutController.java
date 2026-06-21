package com.gymtracker.core.Controller;

import com.gymtracker.core.Repository.AchievementRepository;
import com.gymtracker.core.Repository.ExerciseRepository;
import com.gymtracker.core.Service.WorkoutService;
import com.gymtracker.core.dto.ProgramCreateRequest;
import com.gymtracker.core.dto.TemplateCreateRequest;
import com.gymtracker.core.dto.WorkoutSaveRequest;
import com.gymtracker.core.dto.WorkoutSetDto;
import com.gymtracker.core.entity.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {
    private final WorkoutService workoutService;
    private final ExerciseRepository exerciseRepository;
    private final AchievementRepository achievementRepository;

    public WorkoutController(WorkoutService workoutService, ExerciseRepository exerciseRepository, AchievementRepository achievementRepository) {
        this.workoutService = workoutService;
        this.exerciseRepository = exerciseRepository;
        this.achievementRepository = achievementRepository;
    }

    @PostMapping
    public ResponseEntity<String> saveWorkout(@RequestBody WorkoutSaveRequest workoutSaveRequest) {
        workoutService.setWorkout(workoutSaveRequest);
        return ResponseEntity.ok("Workout saved successfully!");
    }

    @GetMapping("/exercises")
    public ResponseEntity<List<Exercise>> getAllExercises() {
        return ResponseEntity.ok(exerciseRepository.findAll());
    }

    @PostMapping("/programs")
    public ResponseEntity<Long> createProgram(@RequestBody ProgramCreateRequest request) {
        Long programId = workoutService.createTrainingProgram(request);

        return ResponseEntity.ok(programId);
    }

    @PostMapping("/programs/{programId}/templates")
    public ResponseEntity<String> addTemplateToProgram(
            @PathVariable("programId") Long programId,
            @RequestBody TemplateCreateRequest request) {

        workoutService.addTemplateToProgram(programId, request);
        return ResponseEntity.ok("Template added to program successfully!");
    }

    @GetMapping("/programs")
    public ResponseEntity<List<TrainingProgram>> getMyPrograms(@RequestParam("telegramId") Long telegramId) {
        return ResponseEntity.ok(workoutService.getProgramsByUserId(telegramId));
    }

    @DeleteMapping("/programs/{id}")
    public ResponseEntity<String> deleteProgram(@PathVariable("id") Long id){
        workoutService.deleteProgram(id);
        return  ResponseEntity.ok("Program deleted successfully!");
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
            @RequestParam("recipientId") Long recipientId) {
        workoutService.sendFriendRequest(requesterId, recipientId);
        return ResponseEntity.ok("Friend request sent successfully!");
    }

    @PostMapping("/friends/accept")
    public ResponseEntity<String> acceptFriendRequest(
            @RequestParam("requesterId") Long requesterId,
            @RequestParam("recipientId") Long recipientId) {
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


}
