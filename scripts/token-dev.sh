#!/bin/sh
# Fabrica un token de DESARROLLO con las mismas características que el del autenticador (RS256, sub=username).
# Se firma con una clave LOCAL (no la del autenticador): sirve para probar este backend sin depender de él.
# Uso:  sh scripts/token-dev.sh <codigo_de_usuario> [segundos_de_vida] [tipo]
#   sh scripts/token-dev.sh fsepa                      sesión de 30 minutos
#   sh scripts/token-dev.sh fsepa -60                  token ya vencido
#   sh scripts/token-dev.sh fsepa 300 MFA_CHALLENGE    token temporal del paso MFA (debe ser rechazado)
#   sh scripts/token-dev.sh fsepa 604800               vida de 7 días, como el de renovación (debe ser rechazado)
set -eu
cd "$(dirname "$0")/.."
USUARIO="${1:?Uso: sh scripts/token-dev.sh <codigo_de_usuario> [segundos_de_vida] [tipo]}"
VIDA="${2:-1800}"
TIPO="${3:-}"
CARPETA="certs/dev"
mkdir -p "$CARPETA"

if [ ! -f "$CARPETA/jwt-dev-private.pem" ]; then
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$CARPETA/jwt-dev-private.pem" 2>/dev/null
  openssl pkey -in "$CARPETA/jwt-dev-private.pem" -pubout -out "$CARPETA/jwt-dev-public.pem"
  echo "Claves de desarrollo creadas en $CARPETA/ (no se suben a Git)." >&2
  echo "Para que el backend las use, agregue a .env:  JWT_PUBLIC_KEY_FILE=./$CARPETA/jwt-dev-public.pem  y reinicie." >&2
fi

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

AHORA=$(date +%s)
EXP=$((AHORA + VIDA))
if [ -n "$TIPO" ]; then EXTRA=",\"type\":\"$TIPO\""; else EXTRA=""; fi
CABECERA='{"alg":"RS256"}'
CARGA="{\"userId\":1,\"username\":\"$USUARIO\",\"sub\":\"$USUARIO\"$EXTRA,\"iat\":$AHORA,\"exp\":$EXP}"

H=$(printf '%s' "$CABECERA" | b64url)
P=$(printf '%s' "$CARGA" | b64url)
F=$(printf '%s.%s' "$H" "$P" | openssl dgst -sha256 -sign "$CARPETA/jwt-dev-private.pem" | b64url)
printf '%s.%s.%s\n' "$H" "$P" "$F"