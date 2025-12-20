package com.esdc.gameapi.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@AllArgsConstructor
@Builder
@Table(
    name = "progress",
    indexes = {
        @Index(name = "idx_progress_user_level", columnList = "user_id, level_id")
    }
)
public class Progress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "progress_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "user_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_progress_user")
  )
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "level_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_progress_level")
  )
  private Level level;

  @Column(name = "killed_enemies_number", nullable = false)
  private Integer killedEnemiesNumber;

  @Column(name = "solved_puzzles_number", nullable = false)
  private Integer solvedPuzzlesNumber;

  @Column(name = "time_spent", nullable = false)
  private String timeSpent;

  @Column(name = "stars", nullable = false)
  private Integer stars;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Progress() {
  }

  public Progress(User user, Level level) {
    this.user = user;
    this.level = level;
    this.killedEnemiesNumber = 0;
    this.solvedPuzzlesNumber = 0;
    this.timeSpent = "00:00:00";
    this.stars = 0;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
