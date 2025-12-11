package com.esdc.gameApi.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "levels",
        indexes = {
                @Index(name = "idx_levels_name", columnList = "level_name")
        }
)
public class Level {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_id")
    private Long id;

    @Column(name = "level_name", nullable = false, unique = true)
    private String levelName;

    @Column(name = "boss_on_level", nullable = false)
    private Boolean bossOnLevel;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Level() {}

    public Level(String levelName, Boolean bossOnLevel) {
        this.levelName = levelName;
        this.bossOnLevel = bossOnLevel;
    }
}