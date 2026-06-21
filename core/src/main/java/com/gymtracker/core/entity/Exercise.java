package com.gymtracker.core.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymtracker.core.entity.enums.ExerciseType;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.persistence.Enumerated;
@Data
@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) // Игнорирует при чтении файла, но отправляет на телефон!
    private Long id;

    @JsonAlias("name_ru") // Читает из name_ru, но на телефон отправляет как name!
    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @JsonAlias("instruction_ru") // Читает из instruction_ru, но отправляет как description!
    @Column(columnDefinition = "TEXT")
    private String description;

    @JsonAlias("gif_url")
    private String imageURL;

    @JsonAlias("image")
    private String thumbnailURL;

    private String category;
    private String target;

    @JsonAlias("equipment_ru")
    private String equipment;

    @Enumerated(EnumType.STRING)
    private ExerciseType type;
}
