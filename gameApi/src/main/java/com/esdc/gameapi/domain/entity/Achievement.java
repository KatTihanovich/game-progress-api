package com.esdc.gameapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "achievements",
    indexes = {
        @Index(name = "idx_achievements_name", columnList = "achievement_name")
    }
)
public class Achievement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "achievement_id")
  private Long id;

  @Column(name = "achievement_name", nullable = false, unique = true)
  private String achievementName;

  @Column(name = "achievement_description")
  private String achievementDescription;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

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
