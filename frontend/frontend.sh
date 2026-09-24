#!/bin/sh
set -e

cd "$(dirname "$0")"

case "$1" in
  -build)
    docker build --pull -t infra-view-frontend:2026-09-22-01 .
    ;;
  -start)
    docker compose -f ../docker-compose.yml up -d --pull never frontend
    ;;
  *)
    echo "Użycie: ./frontend.sh -build lub -start"
    exit 1
    ;;
esac