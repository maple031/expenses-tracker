CREATE DATABASE IF NOT EXISTS smartspend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smartspend;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS expenses (
  expense_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  category VARCHAR(60) NOT NULL,
  expense_date DATE NOT NULL,
  notes VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT chk_expense_amount CHECK (amount > 0),
  INDEX idx_expenses_user_date (user_id, expense_date)
);

CREATE TABLE IF NOT EXISTS budgets (
  budget_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  budget_month DATE NOT NULL,
  monthly_limit DECIMAL(12,2) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT uq_user_budget_month UNIQUE (user_id, budget_month),
  CONSTRAINT chk_monthly_limit CHECK (monthly_limit > 0)
);

CREATE TABLE IF NOT EXISTS savings_goals (
  goal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  goal_name VARCHAR(120) NOT NULL,
  target_amount DECIMAL(12,2) NOT NULL,
  saved_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  target_date DATE,
  description VARCHAR(500),
  category VARCHAR(60) NOT NULL DEFAULT 'Other',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT chk_goal_target CHECK (target_amount > 0),
  CONSTRAINT chk_goal_saved CHECK (saved_amount >= 0)
);

CREATE TABLE IF NOT EXISTS reminders (
  reminder_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  reminder_text VARCHAR(200) NOT NULL,
  reminder_date DATE NOT NULL,
  is_done BOOLEAN NOT NULL DEFAULT FALSE,
  title VARCHAR(150),
  description VARCHAR(500),
  expected_amount DECIMAL(12,2),
  category VARCHAR(60),
  reminder_time TIME,
  reminder_kind VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_reminders_user_date (user_id, reminder_date)
);

CREATE TABLE IF NOT EXISTS smart_plans (
  plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  activity_type VARCHAR(60) NOT NULL,
  activity_name VARCHAR(150) NOT NULL,
  estimated_cost DECIMAL(12,2) NOT NULL,
  planned_date DATE NOT NULL,
  description VARCHAR(500),
  affordable BOOLEAN NOT NULL DEFAULT TRUE,
  remaining_after DECIMAL(12,2) NOT NULL DEFAULT 0,
  may_exceed_budget BOOLEAN NOT NULL DEFAULT FALSE,
  suggested_limit DECIMAL(12,2) NOT NULL DEFAULT 0,
  suggested_monthly_savings DECIMAL(12,2) NOT NULL DEFAULT 0,
  recommendation_status VARCHAR(20) NOT NULL DEFAULT 'AFFORDABLE',
  recommendation_text TEXT NOT NULL,
  alternatives TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_plans_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  CONSTRAINT chk_plan_cost CHECK (estimated_cost > 0),
  INDEX idx_plans_user_date (user_id, planned_date)
);
