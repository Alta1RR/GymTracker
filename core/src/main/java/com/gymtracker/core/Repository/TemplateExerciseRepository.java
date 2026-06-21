package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.TemplateExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateExerciseRepository extends JpaRepository<TemplateExercise, Long> {
}
