# Données de démo

Base de dev **remise à zéro** puis remplie avec un jeu réaliste (Niger, FCFA) :
3 salons (Plateau et Koubia à Niamey, Maradi), prestations, prestataires avec accès, manager, accueil,
postes, 12 clients avec zone, 60 jours d'historique (réservations, file payée, avis), 7 jours à venir,
file du jour, fidélité (client démo : 1 visite offerte), notes, photos, favoris, un message.

## Procédure

1. Sauvegarde : `pg_dump -h localhost -U postgres -d Test -Fc -f backups/Test-<date>.dump`
2. Schéma neuf : `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`
3. Démarrer l'API (migrations + admin) avec `APP_DEMO_DATA_ENABLED=false` (anciens seeders désactivés).
4. `python tools/demo/seed_demo.py --api http://localhost:8080/api/v1`

Le script refuse de tourner si des salons existent déjà. Les photos référencées doivent être présentes
dans `uploads/user/` (dossier servi par l'API). Jamais en production.

## Comptes

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Admin | admin@barbershop.local | `.env` (APP_ADMIN_PASSWORD) |
| Propriétaire Plateau | owner@demo.barbershop | Owner@123 |
| Propriétaire Koubia | owner.koubia@demo.barbershop | Owner@123 |
| Propriétaire Maradi | owner.maradi@demo.barbershop | Owner@123 |
| Manager (Plateau) | manager@demo.barbershop | Manager@123 |
| Accueil (Plateau) | receptionist@demo.barbershop | Reception@123 |
| Prestataires | presta.ibrahim / abdoul / yacine / souleymane / ismael / habibou / nafiou @demo.barbershop | Presta@123 |
| Client démo | client@demo.barbershop | Client@123 |
| Autres clients | mariama.issa, oumar.diallo, … @demo.barbershop | Client@123 |
