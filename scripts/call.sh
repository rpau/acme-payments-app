#!/usr/bin/env bash
# Usage: scripts/call.sh [amount]   (app must be running on :8080)
AMOUNT=${1:-100}; SECRET=demo-shared-secret; MERCHANT=shop-42; TS=$(date +%s)
SIG=$(printf '%s:%s' "$MERCHANT" "$TS" | openssl dgst -sha256 -hmac "$SECRET" -hex | awk '{print $NF}')
echo "--- POST /payments amount=$AMOUNT"
curl -s -w '\n[HTTP %{http_code}]\n' localhost:8080/payments -H 'Content-Type: application/json' \
  -H "X-Acme-Merchant: $MERCHANT" -H "X-Acme-Timestamp: $TS" -H "X-Acme-Signature: $SIG" \
  -d "{\"amount\":$AMOUNT,\"currency\":\"EUR\"}"
echo "--- POST /payments with bad signature"
curl -s -w '\n[HTTP %{http_code}]\n' localhost:8080/payments -H 'Content-Type: application/json' \
  -H "X-Acme-Merchant: $MERCHANT" -H "X-Acme-Timestamp: $TS" -H "X-Acme-Signature: nope" -d '{}'
echo "--- GET /actuator/health"
curl -s localhost:8080/actuator/health; echo
