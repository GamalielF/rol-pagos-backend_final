#!/bin/sh
# Verifica LOCALMENTE si un token fue firmado con la clave pública configurada.
# Uso:  sh scripts/verificar-token.sh <token> [clave_publica.pem]
# El token está en el navegador: F12 > Application > Session Storage > access_token. No lo comparta.
set -eu
cd "$(dirname "$0")/.."
TOKEN="${1:?Uso: sh scripts/verificar-token.sh <token> [clave_publica.pem]}"
CLAVE="${2:-certs/jwt-public.pem}"
[ -f "$CLAVE" ] || { echo "No existe la clave pública: $CLAVE" >&2; exit 2; }

H=${TOKEN%%.*}; RESTO=${TOKEN#*.}; P=${RESTO%%.*}; F=${RESTO#*.}
decodificar() {
  relleno=$(( (4 - ${#1} % 4) % 4 ))
  { printf '%s' "$1" | tr '_-' '/+'; [ "$relleno" -gt 0 ] && printf '%*s' "$relleno" '' | tr ' ' '='; true; } | openssl base64 -d -A
}
FIRMA=$(mktemp); trap 'rm -f "$FIRMA"' EXIT
decodificar "$F" > "$FIRMA"
echo "Cabecera : $(decodificar "$H")"
echo "Contenido: $(decodificar "$P")"
printf '%s.%s' "$H" "$P" | openssl dgst -sha256 -verify "$CLAVE" -signature "$FIRMA"