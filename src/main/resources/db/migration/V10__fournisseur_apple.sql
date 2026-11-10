-- Connexion Apple : le fournisseur APPLE rejoint LOCAL / GOOGLE / FACEBOOK.
ALTER TABLE application_users DROP CONSTRAINT IF EXISTS application_users_provider_check;
ALTER TABLE application_users ADD CONSTRAINT application_users_provider_check
    CHECK (provider IN ('LOCAL', 'GOOGLE', 'FACEBOOK', 'APPLE'));
