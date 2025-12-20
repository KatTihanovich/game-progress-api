package com.esdc.gameapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Game level entity with configuration and metadata.
 */
@Getter
@Setter
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

  @Column(name = "stars_on_level", nullable = false)
  private Integer starsOnLevel;

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
