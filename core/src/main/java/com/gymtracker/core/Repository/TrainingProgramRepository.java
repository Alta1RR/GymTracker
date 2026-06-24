package com.gymtracker.core.Repository;

import com.gymtracker.core.entity.AppUser;
import com.gymtracker.core.entity.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {
    List<TrainingProgram> findByUser(AppUser user);

    List<TrainingProgram> findByUserIsNull();
}
