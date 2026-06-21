package com.gymtracker.core.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.core.Repository.AchievementRepository;
import com.gymtracker.core.Repository.ExerciseRepository;
import com.gymtracker.core.entity.Achievement;
import com.gymtracker.core.entity.Exercise;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DatabaseInitializer(ExerciseRepository exerciseRepository, AchievementRepository achievementRepository) {
        this.exerciseRepository = exerciseRepository;
        this.achievementRepository = achievementRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (exerciseRepository.count() == 0) {
            System.out.println("=== НАЧАЛО АВТОМАТИЧЕСКОГО ИМПОРТА БАЗЫ УПРАЖНЕНИЙ ===");


            InputStream inputStream = getClass().getResourceAsStream("/exercises_ru.json");


            List<Exercise> exercises = objectMapper.readValue(inputStream, new TypeReference<List<Exercise>>(){});


            String gitHubBaseUrl = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/";


            for (Exercise ex : exercises) {

                if (ex.getEquipment() != null &&
                        (ex.getEquipment().toLowerCase().contains("собствен") ||
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

            System.out.println("=== ИМПОРТ ЗАВЕРШЕН! УСПЕШНО ЗАГРУЖЕНО " + exercises.size() + " УПРАЖНЕНИЙ ===");
        }
    }
}