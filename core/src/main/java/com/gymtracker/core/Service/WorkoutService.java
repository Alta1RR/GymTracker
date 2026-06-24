package com.gymtracker.core.Service;

import com.gymtracker.core.Repository.*;
import com.gymtracker.core.dto.ProgramCreateRequest;
import com.gymtracker.core.dto.TemplateCreateRequest;
import com.gymtracker.core.dto.WorkoutSaveRequest;
import com.gymtracker.core.dto.WorkoutSetDto;
import com.gymtracker.core.entity.*;
import com.gymtracker.core.entity.enums.ExerciseType;
import com.gymtracker.core.entity.enums.FriendshipStatus;
import com.gymtracker.core.entity.enums.ProgressionType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkoutService {
    private final AppUserRepository appUserRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutRepository workoutRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserAchievementRepository userAchievementRepository;

    public WorkoutService(AppUserRepository appUserRepository,
                          ExerciseRepository exerciseRepository,
                          WorkoutRepository workoutRepository,
                          WorkoutSetRepository workoutSetRepository,
                          TrainingProgramRepository trainingProgramRepository,
                          WorkoutTemplateRepository workoutTemplateRepository,
                          TemplateExerciseRepository templateExerciseRepository,
                          FriendshipRepository friendshipRepository,
                          UserAchievementRepository userAchievementRepository) {
        this.appUserRepository = appUserRepository;
        this.workoutRepository = workoutRepository;
        this.exerciseRepository = exerciseRepository;
        this.workoutSetRepository = workoutSetRepository;
        this.trainingProgramRepository = trainingProgramRepository;
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.templateExerciseRepository = templateExerciseRepository;
        this.friendshipRepository = friendshipRepository;
        this.userAchievementRepository = userAchievementRepository;
    }

    @Transactional
    public void setWorkout(WorkoutSaveRequest workoutSaveRequest) {
        AppUser user = appUserRepository.findByTelegramId(workoutSaveRequest.getTelegramId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден! "));

        Workout workout = new Workout();
        workout.setUser(user);
        workout.setName(workoutSaveRequest.getWorkoutName());
        workout.setDurationInSeconds(workoutSaveRequest.getDurationInSeconds());

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
    }

    public Long createTrainingProgram(ProgramCreateRequest programCreateRequest) {
        AppUser user = appUserRepository.findByTelegramId(programCreateRequest.getTelegramId())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден! "));

        TrainingProgram trainingProgram = new TrainingProgram();
        trainingProgram.setUser(user);
        trainingProgram.setName(programCreateRequest.getName());
        trainingProgram.setProgressionType(programCreateRequest.getProgressionType());
        trainingProgram.setCurrentWeek(1);
        trainingProgramRepository.save(trainingProgram);
        return trainingProgram.getId();

    }

    public void addTemplateToProgram(Long programId, TemplateCreateRequest templateCreateRequest) {
        TrainingProgram trainingProgram = trainingProgramRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Программа не найдена"));

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

            templateExerciseRepository.save(templateExercise);
            order++;
        }
    }

    public List<TrainingProgram> getProgramsByUserId(Long telegramId) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return trainingProgramRepository.findByUser(user);
    }

    // Удаление программы
    public void deleteProgram(Long id){
        trainingProgramRepository.deleteById(id);
    }

    public List<WorkoutSetDto> getLatestWorkoutSets(Long telegramId, String workoutName) {
        AppUser user = appUserRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Workout latestWorkout = workoutRepository.findFirstByUserAndNameOrderByDateDesc(user, workoutName)
                .orElse(null);

        if (latestWorkout == null) {
            return new ArrayList<>();
        }

        List<WorkoutSet> lastSets = workoutSetRepository.findByWorkout(latestWorkout);
        if (lastSets.isEmpty()) {
            return new ArrayList<>();
        }
        WorkoutSet bestSet = lastSets.get(0);
        double lastWeight = bestSet.getWeight();
        int lastReps = bestSet.getReps();

        // Базовые переменные для расчетов
        double finalWeight = 0.0;
        int targetReps = 10;
        int targetSetsCount = 3;

        ProgressionType progType = latestWorkout.getTrainingProgram().getProgressionType();

        if (progType == ProgressionType.LINEAR) {
            targetSetsCount = lastSets.size(); // оставляем столько же подходов

            if (bestSet.getExercise().getType() == ExerciseType.BODYWEIGHT) {
                finalWeight = 0.0;
                targetReps = lastReps + 1; // прибавляем 1 повторение для своего веса
            } else {
                finalWeight = lastWeight + 2.5; // прибавляем 2.5 кг для штанги
                targetReps = lastReps;
            }

        } else if (progType == ProgressionType.PERIODIZED) {
            double estimated1RM = lastWeight * (1 + lastReps / 30.0); // Считаем 1ПМ по формуле Эпли
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
                    targetSetsCount = 4; // 4 подхода на тяжелой неделе
                    break;
                case 4:
                    intensity = 0.60;
                    targetReps = 8;
                    targetSetsCount = 2; // 2 подхода на легкой неделе
                    break;
            }

            double calculatedWeight = estimated1RM * intensity;
            finalWeight = Math.round(calculatedWeight / 2.5) * 2.5; // Округляем до 2.5 кг

        } else {
            targetSetsCount = lastSets.size();
            finalWeight = lastWeight;
            targetReps = lastReps;
        }

        List<WorkoutSetDto> targetDtos = new ArrayList<>();
        for (int i = 1; i <= targetSetsCount; i++) {
            WorkoutSetDto dto = new WorkoutSetDto();
            dto.setExerciseId(bestSet.getExercise().getId());
            dto.setSetNumber(i);

            if (bestSet.getExercise().getType() == ExerciseType.BODYWEIGHT) {
                dto.setWeight(0.0);
                if (progType == ProgressionType.PERIODIZED) {
                    double intensity = 1.0;
                    int week = latestWorkout.getTrainingProgram().getCurrentWeek();
                    if (week == 1) intensity = 0.70;
                    else if (week == 2) intensity = 0.775;
                    else if (week == 3) intensity = 0.85;
                    else if (week == 4) intensity = 0.60;

                    dto.setReps((int) Math.round(lastReps * intensity));
                } else {
                    dto.setReps(targetReps);
                }
            } else {
                dto.setWeight(finalWeight);
                dto.setReps(targetReps);
            }

            targetDtos.add(dto);
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
}
