CREATE TABLE trek_groups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  showtime_id BIGINT NOT NULL,
  host_user_id BIGINT NOT NULL,
  description VARCHAR(280),
  max_members INT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  UNIQUE KEY uniq_trek_group_showtime_host (showtime_id, host_user_id),
  INDEX idx_trek_group_showtime (showtime_id),
  INDEX idx_trek_group_host (host_user_id)
);

CREATE TABLE trek_join_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  group_id BIGINT NOT NULL,
  requester_user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  note VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  reviewed_at TIMESTAMP NULL,
  UNIQUE KEY uniq_trek_join_group_requester_status (group_id, requester_user_id, status),
  INDEX idx_trek_join_group (group_id),
  INDEX idx_trek_join_requester (requester_user_id),
  INDEX idx_trek_join_status (status)
);
