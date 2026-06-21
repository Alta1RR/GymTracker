package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.Workout;
import com.gymtracker.core.entity.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {
    List<WorkoutSet> findByWorkout(Workout workout);
}
