-- Flyway migration: initial schema
CREATE TABLE IF NOT EXISTS app_user (
  id UUID PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  external_id VARCHAR(191) NOT NULL,
  username VARCHAR(191) NOT NULL,
  display_name VARCHAR(191),
  email VARCHAR(191),
  avatar_url VARCHAR(512),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_provider_external_id UNIQUE (provider, external_id)
);
