package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
public class Venue {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long cityId;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 255)
  private String address;

  @Column(length = 10)
  private String postalCode;

  @Column(length = 20)
  private String source;

  @Column(length = 120)
  private String sourceId;

  @Column(length = 500)
  private String sourceUrl;
}
