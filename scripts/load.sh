#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
COUNT="${COUNT:-20}"

echo "sending ${COUNT} successful orders to ${BASE_URL}/orders ..."
for _ in $(seq 1 "${COUNT}"); do
  curl -s -X POST "${BASE_URL}/orders" \
    -H "Content-Type: application/json" \
    -d '{"user_id":"user-1","items":[{"sku":"item-1","quantity":2}]}' \
    > /dev/null
done

echo "sending 1 failing order (fail_payment=true) ..."
curl -s -X POST "${BASE_URL}/orders?fail_payment=true" \
  -H "Content-Type: application/json" \
  -d '{"user_id":"user-1","items":[{"sku":"item-1","quantity":2}]}' \
  > /dev/null || true

echo "done"
