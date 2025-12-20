package com.esdc.gameapi.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
    name = "users_achievements",
    indexes = {
        @Index(name = "idx_users_achievements_user_achievement", columnList = "user_id, achievement_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "achievement_id"})
    }
)
public class UserAchievement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_achievement_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_user_achievements_user"))
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "achievement_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_user_achievements_achievement"))
  private Achievement achievement;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public UserAchievement() {
  }

  public UserAchievement(User user, Achievement achievement) {
    this.user = user;
    this.achievement = achievement;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }
}
