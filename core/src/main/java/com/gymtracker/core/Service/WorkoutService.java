package com.gymtracker.core.Service;

import com.gymtracker.core.Repository.*;
import com.gymtracker.core.dto.ProgramCreateRequest;
import com.gymtracker.core.dto.ProgramUpdateRequest;
import com.gymtracker.core.dto.TemplateCreateRequest;
import com.gymtracker.core.dto.TemplateExerciseUpdateRequest;
import com.gymtracker.core.dto.TemplateUpdateRequest;
import com.gymtracker.core.dto.WorkoutSaveRequest;
import com.gymtracker.core.dto.WorkoutSetDto;
import com.gymtracker.core.entity.*;
import com.gymtracker.core.entity.enums.ExerciseType;
import com.gymtracker.core.entity.enums.FriendshipStatus;
import com.gymtracker.core.entity.enums.ProgressionType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.gymtracker.core.dto.FriendProfileResponse;
import com.gymtracker.core.entity.enums.AchievementTrigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkoutService {
    private static final int MAX_USER_PROGRAMS = 50;

    private final AppUserRepository appUserRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutRepository workoutRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementRepository achievementRepository;

    public WorkoutService(AppUserRepository appUserRepository,
                          ExerciseRepository exerciseRepository,
                          WorkoutRepository workoutRepository,
                          WorkoutSetRepository workoutSetRepository,
                          TrainingProgramRepository trainingProgramRepository,
                          WorkoutTemplateRepository workoutTemplateRepository,
                          TemplateExerciseRepository templateExerciseRepository,
                          FriendshipRepository friendshipRepository,
                          UserAchievementRepository userAchievementRepository,
                          AchievementRepository achievementRepository) {
        this.appUserRepository = appUserRepository;
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
        this.workoutSetRepository = workoutSetRepository;
        this.trainingProgramRepository = trainingProgramRepository;
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.templateExerciseRepository = templateExerciseRepository;
        this.friendshipRepository = friendshipRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.achievementRepository = achievementRepository;
    }

    @Transactional
    public void setWorkout(WorkoutSaveRequest workoutSaveRequest) {
        AppUser user = appUserRepository.findByTelegramId(workoutSaveRequest.getTelegramId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден! "));

        Workout workout = new Workout();
        workout.setUser(user);
        workout.setName(workoutSaveRequest.getWorkoutName());
        workout.setDurationInSeconds(workoutSaveRequest.getDurationInSeconds());
        workout.setQualityScorePercent(workoutSaveRequest.getQualityScorePercent());
        workout.setPlannedVolume(workoutSaveRequest.getPlannedVolume());
        workout.setActualVolume(workoutSaveRequest.getActualVolume());

        // Если тренировка находится в рамках к-л программы
        if(workoutSaveRequest.getProgramId() != null){
            TrainingProgram trainingProgram = trainingProgramRepository.findById(workoutSaveRequest
                    .getProgramId()).orElseThrow(() -> new RuntimeException("Программа не найдена"));
            workout.setTrainingProgram(trainingProgram);
            workout.setWeekNumber(trainingProgram.getCurrentWeek());
        }

        Workout savedWorkout = workoutRepository.save(workout);


        for (WorkoutSetDto setDto : workoutSaveRequest.getSets()) {
            Exercise exercise = exerciseRepository.findById(setDto.getExerciseId())
                    .orElseThrow(() -> new RuntimeException("Упражнение не найдено!"));

            WorkoutSet workoutSet = new WorkoutSet();

            workoutSet.setWorkout(savedWorkout);
            workoutSet.setExercise(exercise);

            workoutSet.setSetNumber(setDto.getSetNumber());
            workoutSet.setWeight(setDto.getWeight());
            workoutSet.setReps(setDto.getReps());
            workoutSet.setExtraSet(Boolean.TRUE.equals(setDto.getExtraSet()));

            workoutSetRepository.save(workoutSet);
        }

        // Проверяем, закрыт ли цикл программы
        if(workout.getTrainingProgram() != null){
            TrainingProgram program = workout.getTrainingProgram();
            int totalDaysInProgram = program.getTemplates().size();

            // Количество выполненных дней
            Long completedDaysThisWeek = workoutRepository
                    .countByTrainingProgramAndWeekNumber(program, program.getCurrentWeek());
            // Если выполнены все дни - переключаем счетчик недель
            if(completedDaysThisWeek == totalDaysInProgram){
                int nextWeek = program.getCurrentWeek() +1;

                // Если это был конец четвертой недели - сбрасываем цикл на первую неделю
                if(nextWeek > 4){
                    program.setCurrentWeek(1);
                    // Написать автоматическое 1ПМ пользователя
                }else{
                    program.setCurrentWeek(nextWeek);
                }
                trainingProgramRepository.save(program);
            }
        }
        awardAchievements(user);
    }

    public Long createTrainingProgram(ProgramCreateRequest programCreateRequest) {
        AppUser user = appUserRepository.findByTelegramId(programCreateRequest.getTelegramId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден! "));
        ensureProgramLimitNotExceeded(user);

        TrainingProgram trainingProgram = new TrainingProgram();
        trainingProgram.setUser(user);
        trainingProgram.setName(programCreateRequest.getName());
        trainingProgram.setProgressionType(programCreateRequest.getProgressionType());
        trainingProgram.setCurrentWeek(1);
        trainingProgramRepository.save(trainingProgram);
        return trainingProgram.getId();

    }

    public void addTemplateToProgram(Long programId, Long telegramId, TemplateCreateRequest templateCreateRequest) {
        TrainingProgram trainingProgram = trainingProgramRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Программа не найдена"));
        validateProgramOwner(trainingProgram, telegramId);

        WorkoutTemplate workoutTemplate = new WorkoutTemplate();
        workoutTemplate.setName(templateCreateRequest.getName());
        workoutTemplate.setTrainingProgram(trainingProgram);

        WorkoutTemplate savedTemplate = workoutTemplateRepository.save(workoutTemplate);

        int order = 1;
        for (Long exerciseId : templateCreateRequest.getExerciseIds()) {
            Exercise exercise = exerciseRepository.findById(exerciseId)
                    .orElseThrow(() -> new RuntimeException("Упражнение не найдено"));

            TemplateExercise templateExercise = new TemplateExercise();
            templateExercise.setWorkoutTemplate(savedTemplate);
            templateExercise.setExercise(exercise);
            templateExercise.setSequenceOrder(order);
            templateExercise.setTargetSets(3);

            templateExerciseRepository.save(templateExercise);
            order++;
        }
    }

    public TrainingProgram updateProgram(Long programId, ProgramUpdateRequest request) {
        TrainingProgram program = trainingProgramRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Программа не найдена"));
        validateProgramOwner(program, request.getTelegramId());
        program.setName(request.getName());
        return trainingProgramRepository.save(program);
    }

    @Transactional
    public WorkoutTemplate updateTemplate(Long templateId, TemplateUpdateRequest request) {
        WorkoutTemplate template = workoutTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Тренировочный день не найден"));
        validateProgramOwner(template.getTrainingProgram(), request.getTelegramId());

        template.setName(request.getName());
        WorkoutTemplate savedTemplate = workoutTemplateRepository.save(template);

        List<TemplateExercise> oldExercises = templateExerciseRepository.findByWorkoutTemplate(savedTemplate);
        templateExerciseRepository.deleteAll(oldExercises);

        int order = 1;
        for (TemplateExerciseUpdateRequest item : request.getExercises()) {
            Exercise exercise = exerciseRepository.findById(item.getExerciseId())
                    .orElseThrow(() -> new RuntimeException("Упражнение не найдено"));

            TemplateExercise templateExercise = new TemplateExercise();
            templateExercise.setWorkoutTemplate(savedTemplate);
            templateExercise.setExercise(exercise);
            templateExercise.setSequenceOrder(order++);
            templateExercise.setTargetSets(item.getTargetSets() == null || item.getTargetSets() < 1 ? 1 : item.getTargetSets());
            templateExerciseRepository.save(templateExercise);
        }

        return workoutTemplateRepository.findById(templateId).orElse(savedTemplate);
    }

    public void deleteTemplate(Long templateId, Long telegramId) {
        WorkoutTemplate template = workoutTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Тренировочный день не найден"));
        validateProgramOwner(template.getTrainingProgram(), telegramId);
        workoutTemplateRepository.delete(template);
    }

    public List<TrainingProgram> getProgramsByUserId(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return trainingProgramRepository.findByUser(user);
    }

    // Удаление программы
    public void deleteProgram(Long id, Long telegramId){
        TrainingProgram trainingProgram = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Программа не найдена"));
        validateProgramOwner(trainingProgram, telegramId);
        trainingProgramRepository.delete(trainingProgram);
    }

    @Transactional
    public TrainingProgram copySharedProgram(Long sourceProgramId, Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        TrainingProgram source = trainingProgramRepository.findById(sourceProgramId)
                .orElseThrow(() -> new RuntimeException("Программа не найдена"));

        if (source.getUser() != null && source.getUser().getTelegramId().equals(telegramId)) {
            return source;
        }

        Optional<TrainingProgram> existing = trainingProgramRepository.findByUserAndName(user, source.getName());
        if (existing.isPresent()) {
            return existing.get();
        }
        ensureProgramLimitNotExceeded(user);

        TrainingProgram clone = new TrainingProgram();
        clone.setUser(user);
        clone.setName(source.getName());
        clone.setProgressionType(source.getProgressionType());
        clone.setCurrentWeek(1);
        TrainingProgram savedClone = trainingProgramRepository.save(clone);

        for (WorkoutTemplate sourceTemplate : source.getTemplates()) {
            WorkoutTemplate templateClone = new WorkoutTemplate();
            templateClone.setName(sourceTemplate.getName());
            templateClone.setTrainingProgram(savedClone);
            WorkoutTemplate savedTemplateClone = workoutTemplateRepository.save(templateClone);

            int order = 1;
            for (TemplateExercise sourceExercise : sourceTemplate.getTemplates()) {
                TemplateExercise exerciseClone = new TemplateExercise();
                exerciseClone.setWorkoutTemplate(savedTemplateClone);
                exerciseClone.setExercise(sourceExercise.getExercise());
                exerciseClone.setSequenceOrder(order++);
                exerciseClone.setTargetSets(sourceExercise.getTargetSets());
                templateExerciseRepository.save(exerciseClone);
            }
        }

        return trainingProgramRepository.findById(savedClone.getId()).orElse(savedClone);
    }

    public List<WorkoutSetDto> getLatestWorkoutSets(Long telegramId, String workoutName) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Workout latestWorkout = workoutRepository.findFirstByUserAndNameOrderByDateDesc(user, workoutName)
                .orElse(null);

        if (latestWorkout == null) {
            return new ArrayList<>();
        }

        List<WorkoutSet> lastSets = workoutSetRepository.findByWorkout(latestWorkout)
                .stream()
                .filter(set -> !Boolean.TRUE.equals(set.getExtraSet()))
                .toList();
        if (lastSets.isEmpty()) {
            return new ArrayList<>();
        }

        List<WorkoutSetDto> targetDtos = new ArrayList<>();
        ProgressionType progType = latestWorkout.getTrainingProgram() != null
                ? latestWorkout.getTrainingProgram().getProgressionType()
                : ProgressionType.MANUAL;

        Map<Long, List<WorkoutSet>> setsByExercise = new LinkedHashMap<>();
        for (WorkoutSet set : lastSets) {
            setsByExercise.computeIfAbsent(set.getExercise().getId(), id -> new ArrayList<>()).add(set);
        }

        for (List<WorkoutSet> exerciseSets : setsByExercise.values()) {
            WorkoutSet bestSet = exerciseSets.get(0);
            double lastWeight = bestSet.getWeight();
            int lastReps = bestSet.getReps();
            double finalWeight = lastWeight;
            int targetReps = lastReps;
            int targetSetsCount = exerciseSets.size();

            if (progType == ProgressionType.LINEAR) {
                if (bestSet.getExercise().getType() == ExerciseType.BODYWEIGHT) {
                    finalWeight = 0.0;
                    targetReps = lastReps + 1;
                } else {
                    finalWeight = lastWeight + 2.5;
                }
            } else if (progType == ProgressionType.PERIODIZED) {
                double estimated1RM = lastWeight * (1 + lastReps / 30.0);
                int currentWeek = latestWorkout.getTrainingProgram().getCurrentWeek();
                double intensity = 1.0;

                switch (currentWeek) {
                    case 1:
                        intensity = 0.70;
                        targetReps = 10;
                        targetSetsCount = 3;
                        break;
                    case 2:
                        intensity = 0.775;
                        targetReps = 8;
                        targetSetsCount = 3;
                        break;
                    case 3:
                        intensity = 0.85;
                        targetReps = 5;
                        targetSetsCount = 4;
                        break;
                    case 4:
                        intensity = 0.60;
                        targetReps = 8;
                        targetSetsCount = 2;
                        break;
                }

                if (bestSet.getExercise().getType() == ExerciseType.BODYWEIGHT) {
                    finalWeight = 0.0;
                    targetReps = (int) Math.round(lastReps * intensity);
                } else {
                    double calculatedWeight = estimated1RM * intensity;
                    finalWeight = Math.round(calculatedWeight / 2.5) * 2.5;
                }
            }

            for (int i = 1; i <= targetSetsCount; i++) {
                WorkoutSetDto dto = new WorkoutSetDto();
                dto.setExerciseId(bestSet.getExercise().getId());
                dto.setSetNumber(i);
                dto.setWeight(bestSet.getExercise().getType() == ExerciseType.BODYWEIGHT ? 0.0 : finalWeight);
                dto.setReps(targetReps);
                targetDtos.add(dto);
            }
        }

        return targetDtos;


    }
    // Метод получения истории тренировок
    public List<Workout> getWorkoutHistory(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return workoutRepository.findByUserOrderByDateDesc(user);
    }

    @Transactional
    public void sendFriendRequest(Long requesterId, Long recipientId) {
        AppUser requester = appUserRepository.findByTelegramId(requesterId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        AppUser recipient = appUserRepository.findByTelegramId(recipientId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        // Проверяем, нет ли уже такого запроса в обе стороны (чтобы не дублировать)
        if (friendshipRepository.findByAppUserAndRecipient(requester, recipient).isPresent() ||
                friendshipRepository.findByAppUserAndRecipient(recipient, requester).isPresent()) {
            throw new RuntimeException("Запрос уже отправлен или вы уже друзья");
        }

        Friendship friendship = new Friendship();
        friendship.setAppUser(requester);
        friendship.setRecipient(recipient);
        friendship.setStatus(FriendshipStatus.PENDING); // Статус - Ждет подтверждения

        friendshipRepository.save(friendship);
    }

    @Transactional
    public void acceptFriendRequest(Long requesterId, Long recipientId) {
        AppUser requester = appUserRepository.findByTelegramId(requesterId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден"));
        AppUser recipient = appUserRepository.findByTelegramId(recipientId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден"));

        // Ищем зависший запрос
        Friendship friendship = friendshipRepository.findByAppUserAndRecipient(requester, recipient)
                .orElseThrow(() -> new RuntimeException("Запрос на дружбу не найден"));

        friendship.setStatus(FriendshipStatus.ACCEPTED); // Меняем статус на "Друзья"
        friendshipRepository.save(friendship);
    }

    public List<AppUser> getFriends(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<AppUser> friends = new ArrayList<>();

        // Добавляем тех, кому я отправил запрос и они одобрили
        List<Friendship> sent = friendshipRepository.findByAppUserAndStatus(user, FriendshipStatus.ACCEPTED);
        for (Friendship f : sent) {
            friends.add(f.getRecipient());
        }

        // Добавляем тех, кто мне отправил запрос и я одобрил
        List<Friendship> received = friendshipRepository.findByRecipientAndStatus(user, FriendshipStatus.ACCEPTED);
        for (Friendship f : received) {
            friends.add(f.getAppUser());
        }

        return friends;
    }

    public List<UserAchievement> getEarnedAchievements(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return userAchievementRepository.findByUser(user);
    }

    public List<AppUser> getPendingRequests(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Friendship> pending = friendshipRepository.findByRecipientAndStatus(user, FriendshipStatus.PENDING);
        List<AppUser> requesters = new ArrayList<>();
        for (Friendship f : pending) {
            requesters.add(f.getAppUser()); // забираем отправителя
        }
        return requesters;
    }

    public List<WorkoutTemplate> getPredefinedTemplates() {
        return workoutTemplateRepository.findByTrainingProgramIsNull();
    }
    // ПОЛУЧЕНИЕ ПРЕДУСТАНОВЛЕННЫХ ПРОГРАММ С БЭКЕНДА
    public List<TrainingProgram> getPredefinedPrograms() {
        return trainingProgramRepository.findByUserIsNull();
    }

    // "Запуск" предустановленной программы конкретным пользователем.
    // Предустановленная программа (user = null) - это общий шаблон для всех.
    // Чтобы у каждого пользователя был свой независимый прогресс (своя неделя,
    // свой вес), мы клонируем её в отдельную строку, принадлежащую этому юзеру.
    @Transactional
    public TrainingProgram startPredefinedProgram(Long telegramId, Long presetProgramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        TrainingProgram preset = trainingProgramRepository.findById(presetProgramId)
                .orElseThrow(() -> new RuntimeException("Программа не найдена"));

        if (preset.getUser() != null) {
            throw new RuntimeException("Эта программа уже принадлежит пользователю, клонировать нечего");
        }

        // Если пользователь уже когда-то начинал именно эту стандартную программу -
        // отдаём его существующую копию, а не плодим дубликаты
        Optional<TrainingProgram> existing = trainingProgramRepository.findByUserAndName(user, preset.getName());
        if (existing.isPresent()) {
            return existing.get();
        }
        ensureProgramLimitNotExceeded(user);

        TrainingProgram clone = new TrainingProgram();
        clone.setUser(user);
        clone.setName(preset.getName());
        clone.setProgressionType(preset.getProgressionType());
        clone.setCurrentWeek(1);
        TrainingProgram savedClone = trainingProgramRepository.save(clone);

        for (WorkoutTemplate presetTemplate : preset.getTemplates()) {
            WorkoutTemplate templateClone = new WorkoutTemplate();
            templateClone.setName(presetTemplate.getName());
            templateClone.setTrainingProgram(savedClone);
            WorkoutTemplate savedTemplateClone = workoutTemplateRepository.save(templateClone);

            int order = 1;
            for (TemplateExercise sourceExercise : presetTemplate.getTemplates()) {
                TemplateExercise exerciseClone = new TemplateExercise();
                exerciseClone.setWorkoutTemplate(savedTemplateClone);
                exerciseClone.setExercise(sourceExercise.getExercise());
                exerciseClone.setSequenceOrder(order++);
                exerciseClone.setTargetSets(sourceExercise.getTargetSets());
                templateExerciseRepository.save(exerciseClone);
            }
        }

        // Перечитываем из базы, чтобы в ответе сразу были все склонированные templates
        return trainingProgramRepository.findById(savedClone.getId()).orElse(savedClone);
    }

    @Transactional
    public void resetUserProfile(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        userAchievementRepository.deleteByUser(user);
        friendshipRepository.deleteByAppUserOrRecipient(user, user);
        workoutRepository.deleteByUser(user);
        for (TrainingProgram program : trainingProgramRepository.findByUser(user)) {
            trainingProgramRepository.delete(program);
        }
    }

    public FriendProfileResponse getFriendProfile(Long telegramId, Long friendTelegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        AppUser friend = appUserRepository.findByTelegramId(friendTelegramId)
                .orElseThrow(() -> new RuntimeException("Друг не найден"));

        if (!areFriends(user, friend)) {
            throw new RuntimeException("Профиль доступен только для друзей");
        }

        Long totalWorkouts = workoutRepository.countByUser(friend);
        Long totalSeconds = workoutRepository.sumDurationInSecondsByUser(friend);
        Long totalMinutes = totalSeconds / 60;

        return new FriendProfileResponse(
                friend,
                totalWorkouts,
                totalMinutes,
                workoutRepository.findByUserOrderByDateDesc(friend),
                userAchievementRepository.findByUser(friend)
        );
    }

    private boolean areFriends(AppUser user, AppUser friend) {
        return friendshipRepository.findByAppUserAndRecipient(user, friend)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .isPresent()
                || friendshipRepository.findByAppUserAndRecipient(friend, user)
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .isPresent();
    }

    private void validateProgramOwner(TrainingProgram trainingProgram, Long telegramId) {
        if (trainingProgram.getUser() == null || !trainingProgram.getUser().getTelegramId().equals(telegramId)) {
            throw new RuntimeException("Нет доступа к этой программе");
        }
    }

    private void ensureProgramLimitNotExceeded(AppUser user) {
        if (trainingProgramRepository.countByUser(user) >= MAX_USER_PROGRAMS) {
            throw new RuntimeException("Достигнут лимит: максимум 50 программ на профиль");
        }
    }

    private void awardAchievements(AppUser user) {
        Long workoutCount = workoutRepository.countByUser(user);
        Long totalSeconds = workoutRepository.sumDurationInSecondsByUser(user);
        long totalHours = totalSeconds / 3600;

        for (Achievement achievement : achievementRepository.findAll()) {
            if (userAchievementRepository.existsByUserAndAchievement(user, achievement)) {
                continue;
            }

            if (isAchievementUnlocked(achievement, workoutCount, totalHours)) {
                UserAchievement userAchievement = new UserAchievement();
                userAchievement.setUser(user);
                userAchievement.setAchievement(achievement);
                userAchievementRepository.save(userAchievement);
            }
        }
    }

    private boolean isAchievementUnlocked(Achievement achievement, Long workoutCount, long totalHours) {
        if (achievement.getTriggerType() == AchievementTrigger.WORKOUT_COUNT) {
            return workoutCount >= achievement.getTriggerValue();
        }

        if (achievement.getTriggerType() == AchievementTrigger.GYM_HOURS) {
            return totalHours >= achievement.getTriggerValue();
        }

        return false;
    }
}
