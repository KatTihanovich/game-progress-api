package com.esdc.gameapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
  private String totalTimePlayed;

  @Column(name = "total_killed_enemies", nullable = false)
  private Integer totalKilledEnemies;

  @Column(name = "total_solved_puzzles", nullable = false)
  private Integer totalSolvedPuzzles;

  @Column(name = "total_stars", nullable = false)
  private Integer totalStars;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public UserStatistics(User user) {
    this.user = user;
    this.totalLevelsCompleted = 0;
    this.totalTimePlayed = "00:00:00";
    this.totalKilledEnemies = 0;
    this.totalSolvedPuzzles = 0;
    this.totalStars = 0;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}
