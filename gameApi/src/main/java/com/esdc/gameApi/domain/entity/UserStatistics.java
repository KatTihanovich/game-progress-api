package com.esdc.gameApi.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(
        name = "users_statistics",
        indexes = {
                @Index(name = "idx_users_statistics_user_id", columnList = "user_id")
        }
)
public class UserStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statistics_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_statistics_user"))
    private User user;

    @Column(name = "total_levels_completed", nullable = false)
    private Integer totalLevelsCompleted;

    @Column(name = "total_time_played", nullable = false)
    private String totalTimePlayed; // формат HH:MM:SS

    @Column(name = "total_killed_enemies", nullable = false)
    private Integer totalKilledEnemies;

    @Column(name = "total_solved_puzzles", nullable = false)
    private Integer totalSolvedPuzzles;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public UserStatistics() {}

    public UserStatistics(User user) {
        this.user = user;
        this.totalLevelsCompleted = 0;
        this.totalTimePlayed = "00:00:00";
        this.totalKilledEnemies = 0;
        this.totalSolvedPuzzles = 0;
    }
}