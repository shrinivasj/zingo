package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {
  @Id
  private Long userId;

  @Column(nullable = false, length = 80)
  private String displayName;

  @Column(columnDefinition = "mediumtext")
  private String avatarUrl;

  @Column(length = 140)
  private String bioShort;

  @Column(columnDefinition = "mediumtext")
  private String e2eePublicKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private List<String> personalityTags;
}
