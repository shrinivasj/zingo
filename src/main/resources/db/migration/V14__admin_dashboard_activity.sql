CREATE TABLE admin_sync_runs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id BIGINT NULL,
  city_name VARCHAR(120) NULL,
  postal_code VARCHAR(20) NULL,
  days_requested INT NULL,
  venues_upserted INT NOT NULL DEFAULT 0,
  events_upserted INT NOT NULL DEFAULT 0,
  showtimes_upserted INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_admin_sync_runs_created_at (created_at),
  INDEX idx_admin_sync_runs_actor (actor_user_id)
);

CREATE TABLE admin_activity_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id BIGINT NULL,
  action_type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  detail VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_admin_activity_logs_created_at (created_at),
  INDEX idx_admin_activity_logs_actor (actor_user_id),
  INDEX idx_admin_activity_logs_action_type (action_type)
);
