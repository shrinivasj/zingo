CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE profiles (
  user_id BIGINT PRIMARY KEY,
  display_name VARCHAR(80) NOT NULL,
  avatar_url VARCHAR(500),
  bio_short VARCHAR(140),
  personality_tags JSON
);

CREATE TABLE cities (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL
);

CREATE TABLE venues (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  city_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL
);

CREATE TABLE events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(20) NOT NULL,
  title VARCHAR(200) NOT NULL,
  poster_url VARCHAR(500)
);

CREATE TABLE showtimes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  venue_id BIGINT NOT NULL,
  starts_at TIMESTAMP NOT NULL,
  format VARCHAR(20) NOT NULL
);

CREATE TABLE lobby_presence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  showtime_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  last_seen_at TIMESTAMP NOT NULL,
  UNIQUE KEY uniq_lobby_showtime_user (showtime_id, user_id),
  INDEX idx_lobby_showtime (showtime_id),
  INDEX idx_lobby_user (user_id)
);

CREATE TABLE invites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  from_user_id BIGINT NOT NULL,
  to_user_id BIGINT NOT NULL,
  showtime_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_invite_from (from_user_id),
  INDEX idx_invite_to (to_user_id)
);

CREATE TABLE conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  showtime_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE conversation_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  UNIQUE KEY uniq_conv_member (conversation_id, user_id),
  INDEX idx_conv_member_user (user_id)
);

CREATE TABLE messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  text VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  INDEX idx_message_conversation (conversation_id)
);

CREATE TABLE notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  payload_json JSON,
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  INDEX idx_notification_user (user_id)
);

CREATE TABLE blocks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  blocker_id BIGINT NOT NULL,
  blocked_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  INDEX idx_block_blocker (blocker_id),
  INDEX idx_block_blocked (blocked_id)
);

CREATE TABLE reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  reported_id BIGINT NOT NULL,
  reason VARCHAR(120) NOT NULL,
  details VARCHAR(1000),
  created_at TIMESTAMP NOT NULL,
  INDEX idx_report_reporter (reporter_id),
  INDEX idx_report_reported (reported_id)
);
