CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY, demo_key VARCHAR(32) NOT NULL UNIQUE, display_name VARCHAR(80) NOT NULL
);
CREATE TABLE IF NOT EXISTS template (
  id VARCHAR(64) PRIMARY KEY, category VARCHAR(80) NOT NULL, title VARCHAR(120) NOT NULL,
  description VARCHAR(500) NOT NULL, cover_path VARCHAR(255) NOT NULL,
  result_sample_path VARCHAR(255) NOT NULL, required_image_count INT NOT NULL,
  accepted_mime_types VARCHAR(255) NOT NULL, max_file_size_bytes BIGINT NOT NULL,
  accent VARCHAR(32) NOT NULL, badge VARCHAR(32), is_enabled TINYINT NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS asset (
  id CHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, original_filename VARCHAR(255) NOT NULL,
  mime_type VARCHAR(32) NOT NULL, size_bytes BIGINT NOT NULL, storage_key VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_asset_user (user_id), FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE TABLE IF NOT EXISTS production_task (
  id CHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, template_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL, current_stage VARCHAR(20), result_storage_key VARCHAR(255),
  failure_code VARCHAR(64), failure_message VARCHAR(500), simulation_fail_stage VARCHAR(20),
  simulation_delay_ms INT NOT NULL, created_at DATETIME(6) NOT NULL, started_at DATETIME(6),
  finished_at DATETIME(6), updated_at DATETIME(6) NOT NULL,
  INDEX idx_task_user_created (user_id, created_at),
  FOREIGN KEY (user_id) REFERENCES app_user(id), FOREIGN KEY (template_id) REFERENCES template(id)
);
CREATE TABLE IF NOT EXISTS task_asset (
  task_id CHAR(36) NOT NULL, slot_index INT NOT NULL, asset_id CHAR(36) NOT NULL,
  PRIMARY KEY (task_id, slot_index), FOREIGN KEY (task_id) REFERENCES production_task(id),
  FOREIGN KEY (asset_id) REFERENCES asset(id)
);
CREATE TABLE IF NOT EXISTS task_step (
  task_id CHAR(36) NOT NULL, stage VARCHAR(20) NOT NULL, step_order INT NOT NULL,
  status VARCHAR(20) NOT NULL, started_at DATETIME(6), finished_at DATETIME(6),
  failure_code VARCHAR(64), failure_message VARCHAR(500),
  PRIMARY KEY (task_id, stage), FOREIGN KEY (task_id) REFERENCES production_task(id)
);
