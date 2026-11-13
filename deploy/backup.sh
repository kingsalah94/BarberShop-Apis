#!/bin/sh
# Sauvegarde quotidienne PostgreSQL (lancée par le service « backup » du compose).
# Fichiers : volume db-backups → /backups/barbershop-AAAA-MM-JJ-HHMM.sql.gz
# Conservation : 14 jours.
# Restaurer : gunzip -c fichier.sql.gz | docker compose -f docker-compose.prod.yml exec -T db psql -U barbershop barbershop
set -u
while true; do
  f="/backups/barbershop-$(date +%F-%H%M).sql.gz"
  if pg_dump --no-owner | gzip > "$f"; then
    echo "Sauvegarde OK : $f"
  else
    echo "Sauvegarde ÉCHOUÉE : $f" >&2
  fi
  find /backups -name 'barbershop-*.sql.gz' -mtime +14 -exec rm -f {} \;
  sleep 86400
done
