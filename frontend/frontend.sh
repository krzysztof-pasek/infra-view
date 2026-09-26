#!/bin/sh
set -e

cd "$(dirname "$0")"

case "${1:-}" in
  -build)
    image="infra-view-frontend:$(date -u +%Y-%m-%d-%H%M%S)"
    docker build --pull -t "$image" -t infra-view-frontend:local .
    echo "Zbudowano: $image"
    ;;
  -start)
    docker compose -f ../docker-compose.yml up -d --pull never frontend
    ;;
  *)
    echo "Użycie: ./frontend.sh -build lub -start"
    exit 1
    ;;
esac