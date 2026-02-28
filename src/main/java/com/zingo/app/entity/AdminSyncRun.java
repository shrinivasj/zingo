package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_sync_runs")
public class AdminSyncRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private Long actorUserId;

  @Column(length = 120)
  private String cityName;

  @Column(length = 20)
  private String postalCode;

  @Column
  private Integer daysRequested;

  @Column(nullable = false)
  private Integer venuesUpserted = 0;

  @Column(nullable = false)
  private Integer eventsUpserted = 0;

  @Column(nullable = false)
  private Integer showtimesUpserted = 0;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private Instant createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getActorUserId() { return actorUserId; }
  public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
  public String getCityName() { return cityName; }
  public void setCityName(String cityName) { this.cityName = cityName; }
  public String getPostalCode() { return postalCode; }
  public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
  public Integer getDaysRequested() { return daysRequested; }
  public void setDaysRequested(Integer daysRequested) { this.daysRequested = daysRequested; }
  public Integer getVenuesUpserted() { return venuesUpserted; }
  public void setVenuesUpserted(Integer venuesUpserted) { this.venuesUpserted = venuesUpserted; }
  public Integer getEventsUpserted() { return eventsUpserted; }
  public void setEventsUpserted(Integer eventsUpserted) { this.eventsUpserted = eventsUpserted; }
  public Integer getShowtimesUpserted() { return showtimesUpserted; }
  public void setShowtimesUpserted(Integer showtimesUpserted) { this.showtimesUpserted = showtimesUpserted; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
