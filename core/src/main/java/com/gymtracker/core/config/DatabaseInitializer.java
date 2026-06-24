package com.gymtracker.core.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.core.Repository.AchievementRepository;
import com.gymtracker.core.Repository.ExerciseRepository;
import com.gymtracker.core.Repository.WorkoutTemplateRepository;
import com.gymtracker.core.Repository.TemplateExerciseRepository;
import com.gymtracker.core.entity.Achievement;
import com.gymtracker.core.entity.Exercise;
import com.gymtracker.core.entity.WorkoutTemplate;
import com.gymtracker.core.entity.TemplateExercise;
import com.gymtracker.core.entity.enums.AchievementTrigger;
import com.gymtracker.core.entity.enums.ExerciseType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final ExerciseRepository exerciseRepository;
    private final AchievementRepository achievementRepository;
    private final WorkoutTemplateRepository workoutTemplateRepository;
    private final TemplateExerciseRepository templateExerciseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DatabaseInitializer(ExerciseRepository exerciseRepository,
                               AchievementRepository achievementRepository,
                               WorkoutTemplateRepository workoutTemplateRepository,
                               TemplateExerciseRepository templateExerciseRepository) {
        this.exerciseRepository = exerciseRepository;
        this.achievementRepository = achievementRepository;
        this.workoutTemplateRepository = workoutTemplateRepository;
        this.templateExerciseRepository = templateExerciseRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. ИМПОРТ БАЗЫ УПРАЖНЕНИЙ
        if (exerciseRepository.count() == 0) {
            System.out.println("=== НАЧАЛО АВТОМАТИЧЕСКОГО ИМПОРТА БАЗЫ УПРАЖНЕНИЙ ===");

            InputStream inputStream = getClass().getResourceAsStream("/exercises_ru.json");
            List<Exercise> exercises = objectMapper.readValue(inputStream, new TypeReference<List<Exercise>>(){});

            String gitHubBaseUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/";

            for (Exercise ex : exercises) {
                if (ex.getEquipment() != null && (ex.getEquipment().toLowerCase().contains("собствен") ||
                        ex.getEquipment().toLowerCase().contains("вес") ||
                        ex.getEquipment().toLowerCase().contains("body"))) {
                    ex.setType(ExerciseType.BODYWEIGHT);
                } else {
                    ex.setType(ExerciseType.WEIGHTED);
                }

                if (ex.getImageURL() != null) {
                    ex.setImageURL(gitHubBaseUrl + ex.getImageURL());
                }
                if (ex.getThumbnailURL() != null) {
                    ex.setThumbnailURL(gitHubBaseUrl + ex.getThumbnailURL());
                }
            }
            exerciseRepository.saveAll(exercises);
            System.out.println("=== ИМПОРТ ЗАВЕРШЕН! УСПЕШНО ЗАГРУЖЕНО " + exercises.size() + " УПРАЖНЕНИЙ ===");
        }

        // 2. НАПОЛНЯЕМ СТАНДАРТНЫЕ ТРЕНИРОВКИ (Исправлено условие: проверяем только стандартные тренировки, а не кастомные)
        if (workoutTemplateRepository.findByTrainingProgramIsNull().isEmpty()) {
            System.out.println("=== НАЧАЛО ИМПОРТА СТАНДАРТНЫХ ШАБЛОНОВ ===");

            createPredefinedTemplate("FullBody Классика (Дефолт)",
                    new String[]{"жим штанги лежа", "приседания со штангой", "подтягивания"});

            createPredefinedTemplate("Верх тела (Сила и Объем)",
                    new String[]{"жим штанги лежа", "тяга штанги в наклоне", "армейский жим"});

            createPredefinedTemplate("Низ тела (Мощный старт)",
                    new String[]{"приседания со штангой", "становая тяга", "выпады"});

            createPredefinedTemplate("Пресс и Кор (Стальной пресс)",
                    new String[]{"скручивания", "планка"});

            System.out.println("=== ИМПОРТ СТАНДАРТНЫХ ШАБЛОНОВ ЗАВЕРШЕН ===");
        }

        // 3. НАПОЛНЯЕМ ДОСТИЖЕНИЯ
        if (achievementRepository.count() == 0) {
            System.out.println("=== НАЧАЛО ИМПОРТА ДОСТИЖЕНИЙ ===");
            Achievement a1 = new Achievement();
            a1.setName("Первые шаги");
            a1.setDescription("Выполнил 5 тренировок");
            a1.setIcon("🥇");
            a1.setTriggerType(AchievementTrigger.WORKOUT_COUNT);
            a1.setTriggerValue(5);

            Achievement a2 = new Achievement();
            a2.setName("Режим активирован");
            a2.setDescription("Выполнил 10 тренировок");
            a2.setIcon("💪");
            a2.setTriggerType(AchievementTrigger.WORKOUT_COUNT);
            a2.setTriggerValue(10);

            Achievement a3 = new Achievement();
            a3.setName("Постоянный клиент");
            a3.setDescription("Провел суммарно 2 часа в зале под нагрузкой");
            a3.setIcon("⏱");
            a3.setTriggerType(AchievementTrigger.GYM_HOURS);
            a3.setTriggerValue(2); // 2 часа

            achievementRepository.saveAll(List.of(a1, a2, a3));
            System.out.println("=== ИМПОРТ ДОСТИЖЕНИЙ ЗАВЕРШЕН! ===");
        }
    }

    // Вспомогательный метод для создания стандартных шаблонов
    private void createPredefinedTemplate(String templateName, String[] exerciseQueries) {
        WorkoutTemplate template = new WorkoutTemplate();
        template.setName(templateName);
        template.setTrainingProgram(null); // NULL означает глобальную стандартную тренировку

        WorkoutTemplate savedTemplate = workoutTemplateRepository.save(template);

        int order = 1;
        for (String query : exerciseQueries) {
            Exercise ex = findExerciseByName(query);
            if (ex != null) {
                TemplateExercise te = new TemplateExercise();
                te.setWorkoutTemplate(savedTemplate);
                te.setExercise(ex);
                te.setSequenceOrder(order++);
                templateExerciseRepository.save(te);
            } else {
                System.out.println("Предупреждение: упражнение по запросу \"" + query + "\" не найдено в базе.");
            }
        }
    }

    private Exercise findExerciseByName(String name) {
        return exerciseRepository.findAll().stream()
                .filter(e -> e.getName().toLowerCase().contains(name.toLowerCase()))
                .findFirst()
                .orElse(null);
    }
}