ALTER TABLE profiles
  ADD COLUMN e2ee_encrypted_private_key MEDIUMTEXT NULL,
  ADD COLUMN e2ee_key_salt VARCHAR(255) NULL;
