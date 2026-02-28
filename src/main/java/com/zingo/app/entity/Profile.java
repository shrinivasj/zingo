package com.zingo.app.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
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

  @Column(columnDefinition = "mediumtext")
  @JsonIgnore
  private String e2eeEncryptedPrivateKey;

  @Column(length = 255)
  @JsonIgnore
  private String e2eeKeySalt;

  @Column(nullable = false)
  private boolean trekHostEnabled;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private List<String> personalityTags;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public String getBioShort() {
    return bioShort;
  }

  public void setBioShort(String bioShort) {
    this.bioShort = bioShort;
  }

  public String getE2eePublicKey() {
    return e2eePublicKey;
  }

  public void setE2eePublicKey(String e2eePublicKey) {
    this.e2eePublicKey = e2eePublicKey;
  }

  public String getE2eeEncryptedPrivateKey() {
    return e2eeEncryptedPrivateKey;
  }

  public void setE2eeEncryptedPrivateKey(String e2eeEncryptedPrivateKey) {
    this.e2eeEncryptedPrivateKey = e2eeEncryptedPrivateKey;
  }

  public String getE2eeKeySalt() {
    return e2eeKeySalt;
  }

  public void setE2eeKeySalt(String e2eeKeySalt) {
    this.e2eeKeySalt = e2eeKeySalt;
  }

  public boolean isTrekHostEnabled() {
    return trekHostEnabled;
  }

  public void setTrekHostEnabled(boolean trekHostEnabled) {
    this.trekHostEnabled = trekHostEnabled;
  }

  public List<String> getPersonalityTags() {
    return personalityTags;
  }

  public void setPersonalityTags(List<String> personalityTags) {
    this.personalityTags = personalityTags;
  }
}
