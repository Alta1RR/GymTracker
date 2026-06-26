package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.TemplateExercise;
import com.gymtracker.core.entity.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {
    List<TemplateExercise> findByWorkoutTemplate(WorkoutTemplate workoutTemplate);
}
