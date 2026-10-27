#!/bin/sh
# Levanta MySQL y deja Vault listo (secretos repuestos). Úsalo cada vez que enciendas el equipo.
set -eu
cd "$(dirname "$0")/.."
sudo service docker start >/dev/null 2>&1 || true
docker compose --env-file .env -f docker/docker-compose.yml up -d mysql
docker compose --env-file .env -f docker/docker-compose.yml up -d --force-recreate vault vault-init
echo "Esperando a que Vault termine de sembrar..."
docker wait midas-rhh-vault-init >/dev/null
docker logs --tail 2 midas-rhh-vault-init