#!/bin/sh
# Prepara el Vault de DESARROLLO. Es idempotente: se puede ejecutar cuantas veces se quiera.
set -eu

APP="rol-pagos-backend"
MOUNT="secret"

echo "[vault-init] Esperando a Vault en ${VAULT_ADDR} ..."
intentos=0
until vault status >/dev/null 2>&1; do
  intentos=$((intentos + 1))
  if [ "$intentos" -ge 30 ]; then
    echo "[vault-init] ERROR: Vault no responde"
    exit 1
  fi
  sleep 1
done

[ -n "${DB_USER:-}" ] && [ -n "${DB_PASSWORD:-}" ] || {
  echo "[vault-init] ERROR: DB_USER y DB_PASSWORD son obligatorios"
  exit 1
}

# 1) Motor de secretos KV v2 en secret/ (en modo dev ya viene montado)
if ! vault secrets list -format=json | grep -q "\"${MOUNT}/\""; then
  vault secrets enable -path="${MOUNT}" -version=2 kv
fi

# 2) Política: la aplicación SOLO puede leer sus propios secretos
vault policy write "${APP}" - <<POLITICA
path "${MOUNT}/data/${APP}/*" {
  capabilities = ["read"]
}
path "${MOUNT}/metadata/${APP}/*" {
  capabilities = ["read", "list"]
}
POLITICA

# 3) AppRole: así se autentica la aplicación (sin usar el token raíz)
if ! vault auth list -format=json | grep -q '"approle/"'; then
  vault auth enable approle
fi
vault write "auth/approle/role/${APP}" \
  token_policies="${APP}" \
  token_ttl=1h \
  token_max_ttl=4h \
  secret_id_ttl=0 \
  secret_id_num_uses=0 >/dev/null

# 4) Secretos (semilla tomada del .env)
vault kv put "${MOUNT}/${APP}/db" "db.username=${DB_USER}" "db.password=${DB_PASSWORD}" >/dev/null

# Datos del AD (la URL interna y el token NUNCA van al repositorio)
[ -n "${AD_URL_BASE:-}" ] || { echo "[vault-init] ERROR: AD_URL_BASE es obligatorio"; exit 1; }
if [ -n "${AD_TOKEN:-}" ]; then
  vault kv put "${MOUNT}/${APP}/ad" "ad.url-base=${AD_URL_BASE}" "ad.token=${AD_TOKEN}" >/dev/null
else
  vault kv put "${MOUNT}/${APP}/ad" "ad.url-base=${AD_URL_BASE}" >/dev/null
fi
# 5) Credenciales de la aplicación, entregadas como archivo de propiedades de Spring
ROLE_ID=$(vault read -field=role_id "auth/approle/role/${APP}/role-id")
SECRET_ID=$(vault write -f -field=secret_id "auth/approle/role/${APP}/secret-id")

mkdir -p /runtime
{
  echo "spring.cloud.vault.app-role.role-id=${ROLE_ID}"
  echo "spring.cloud.vault.app-role.secret-id=${SECRET_ID}"
} > /runtime/vault-approle.properties

echo "[vault-init] Listo: política, AppRole y secretos creados."