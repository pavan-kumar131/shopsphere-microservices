#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
wait_http() {
  local url="$1" attempts="${2:-30}"
  for _ in $(seq 1 "$attempts"); do
    if curl --silent --show-error --fail "$url" >/dev/null; then echo "PASS $url"; return 0; fi
    sleep 3
  done
  echo "FAIL $url did not become healthy" >&2; return 1
}
wait_http http://localhost:8080/actuator/health 40
wait_http http://localhost:3001/ 40
wait_http http://localhost:3001/seller/ 40
wait_http http://localhost:3001/admin/ 40
wait_http http://localhost:8080/api/products 20
for service in auth-service iam-service product-service order-service payment-service notification-service; do
  echo "== $service health =="
  docker compose exec -T "$service" sh -lc 'command -v curl >/dev/null && curl -fsS http://127.0.0.1:'"$(case "$service" in auth-service) echo 8081;; iam-service) echo 8087;; product-service) echo 8082;; order-service) echo 8084;; payment-service) echo 8085;; notification-service) echo 8086;; esac)"'/actuator/health'
done
echo 'Smoke tests passed. This validates service health and routes, not the full checkout/payment lifecycle.'
