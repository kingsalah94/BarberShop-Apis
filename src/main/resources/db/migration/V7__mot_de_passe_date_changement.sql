-- Date du dernier changement de mot de passe (expiration configurable dans Paramètres › Sécurité).
ALTER TABLE application_users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
-- Les comptes existants partent de leur date de création (pas d'expiration rétroactive brutale).
UPDATE application_users SET password_changed_at = COALESCE(updated_at, created_at) WHERE password_changed_at IS NULL;
