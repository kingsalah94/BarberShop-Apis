-- ════════════════════════════════════════════════════════════════════════════
--  V3 — Retrait de l'ancien modèle « barbier » (unification « tout salon »).
--  La réservation passe exclusivement par salon_bookings / salon_services ;
--  les prestataires sont des employees. Sauvegarder la base avant d'appliquer.
--  Idempotent (IF EXISTS).
-- ════════════════════════════════════════════════════════════════════════════

-- Liens de la file d'attente et de la messagerie vers l'ancien modèle
DROP TABLE IF EXISTS queue_entry_services;
ALTER TABLE queue_entries DROP COLUMN IF EXISTS booking_id;
ALTER TABLE queue_entries DROP COLUMN IF EXISTS preferred_barber_id;
ALTER TABLE queue_entries DROP COLUMN IF EXISTS assigned_barber_id;
ALTER TABLE chat_messages DROP COLUMN IF EXISTS booking_id;

-- Avis, favoris et disponibilités « par barbier »
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS client_favorites;
DROP TABLE IF EXISTS availability_available_slots;
DROP TABLE IF EXISTS availability_booked_slots;
DROP TABLE IF EXISTS availabilities;

-- Réservations et prestations « par barbier »
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS barber_service;
