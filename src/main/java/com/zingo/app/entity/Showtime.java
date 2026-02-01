package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "showtimes")
@Getter
@Setter
@NoArgsConstructor
public class Showtime {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long eventId;

  @Column(nullable = false)
  private Long venueId;

  @Column(nullable = false)
  private Instant startsAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
  private ShowFormat format;

  @Column(length = 20)
  private String source;

  @Column(length = 120)
  private String sourceId;

  @Column(length = 500)
  private String sourceUrl;
}
