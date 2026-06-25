package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.Exercise;
import com.gymtracker.core.entity.TrainingProgram;
import com.gymtracker.core.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    Optional<Workout> findFirstByUserAndNameOrderByDateDesc(AppUser appUser, String name);

    Long countByTrainingProgramAndWeekNumber(TrainingProgram trainingProgram, Integer weekNumber);

    @Query("SELECT MAX(ws.weight * (1 + ws.reps / 30.0)) " +
            "FROM WorkoutSet ws " +
            "WHERE ws.workout.user = :user AND ws.exercise = :exercise")
    Double findMax1RM(@Param("user") AppUser user, @Param("exercise") Exercise exercise);

    List<Workout> findByUserOrderByDateDesc(AppUser user);

    Long countByUser(AppUser user);

    @Query("SELECT COALESCE(SUM(w.durationInSeconds), 0) FROM Workout w WHERE w.user = :user")
    Long sumDurationInSecondsByUser(@Param("user") AppUser user);
}
