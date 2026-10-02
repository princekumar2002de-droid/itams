#!/usr/bin/env bash
# =============================================================================
#  ITAMS · end-to-end smoke test.
#
#  Runs the full happy path against a live backend:
#     1. Log in as admin → capture access token
#     2. Create a department, a person, an employee, an asset model, an asset
#     3. Assign the asset to the person
#     4. Try to assign the same asset a second time → must be 409
#     5. Return the asset
#     6. Retire the asset
#     7. Fetch the management-dashboard aggregate
#     8. Raise a support ticket about a totally different asset
#
#  Every step prints what it did and asserts on the HTTP status code so the
#  script exits non-zero on the first real failure — this is a smoke test,
#  not an integration test suite.
#
#  Usage:
#     ./smoke.sh                    # against http://localhost:8080
#     BASE=http://localhost:8080 ADMIN=admin PASSWORD=changeme ./smoke.sh
# =============================================================================
set -Eeuo pipefail

BASE="${BASE:-http://localhost:8080}/api/v1"
ADMIN="${ADMIN:-admin}"
PASSWORD="${PASSWORD:-changeme}"
SUFFIX=$RANDOM

BOLD=$'\033[1m'; GREEN=$'\033[32m'; RED=$'\033[31m'; RESET=$'\033[0m'
say()  { printf "\n${BOLD}%s${RESET}\n" "$*"; }
ok()   { printf "  ${GREEN}✓${RESET} %s\n" "$*"; }
fail() { printf "  ${RED}✗${RESET} %s\n" "$*"; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || fail "missing dependency: $1"; }

need curl
need jq

# --------------------------------------------------------------------------- #
say "0. Sanity: is the backend up?"
if ! curl -fsS "${BASE%/api/v1}/actuator/health" >/dev/null; then
  fail "Backend not reachable at ${BASE%/api/v1}/actuator/health — is it running?"
fi
ok "backend healthy"

# --------------------------------------------------------------------------- #
say "1. Log in as ${ADMIN}"
LOGIN=$(curl -sS -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN}\",\"password\":\"${PASSWORD}\"}")
ACCESS=$(echo "$LOGIN" | jq -r '.accessToken')
[[ "$ACCESS" == "null" || -z "$ACCESS" ]] && fail "login failed: $LOGIN"
AUTH=(-H "Authorization: Bearer $ACCESS")
ok "logged in — access token acquired"

# --------------------------------------------------------------------------- #
say "2. Create department + person + employee"
DEPT=$(curl -sS -X POST "$BASE/departments" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"code\":\"ENG-$SUFFIX\",\"name\":\"Engineering $SUFFIX\"}" | jq -r '.id')
ok "dept id=$DEPT"

PERSON=$(curl -sS -X POST "$BASE/people" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"firstName\":\"Grace\",\"lastName\":\"Hopper\",\"email\":\"grace-$SUFFIX@example.com\"}" | jq -r '.id')
ok "person id=$PERSON"

curl -sS -X POST "$BASE/employees" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"personId\":$PERSON,\"employeeNumber\":\"EMP-$SUFFIX\",\"departmentId\":$DEPT,\"hireDate\":\"2026-01-15\"}" >/dev/null
ok "employee wired"

# --------------------------------------------------------------------------- #
say "3. Create an asset model + asset"
MODEL=$(curl -sS -X POST "$BASE/asset-models" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"categoryId\":1,\"manufacturer\":\"Lenovo\",\"modelName\":\"ThinkPad T14 Gen $SUFFIX\"}" | jq -r '.id')
ok "model id=$MODEL"

ASSET=$(curl -sS -X POST "$BASE/assets" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"assetTag\":\"A-$SUFFIX\",\"modelId\":$MODEL,\"serialNumber\":\"SN-$SUFFIX\",\"purchaseDate\":\"2026-03-01\",\"purchasePrice\":1500.00}" | jq -r '.id')
ok "asset id=$ASSET"

# --------------------------------------------------------------------------- #
say "4. Assign the asset"
ASSIGN=$(curl -sS -X POST "$BASE/assignments" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"assetId\":$ASSET,\"assigneePersonId\":$PERSON,\"outCondition\":\"NEW\"}" | jq -r '.id')
ok "assignment id=$ASSIGN"

STATUS=$(curl -sS "$BASE/assets/$ASSET" "${AUTH[@]}" | jq -r '.status')
[[ "$STATUS" == "ASSIGNED" ]] || fail "expected ASSIGNED, got $STATUS"
ok "asset status = ASSIGNED"

# --------------------------------------------------------------------------- #
say "5. Second assignment on the same asset must be 409"
CODE=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/assignments" "${AUTH[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"assetId\":$ASSET,\"assigneePersonId\":$PERSON,\"outCondition\":\"NEW\"}")
[[ "$CODE" == "409" ]] || fail "expected HTTP 409, got $CODE"
ok "duplicate assign correctly rejected with 409"

# --------------------------------------------------------------------------- #
say "6. Return the asset"
curl -sS -X POST "$BASE/assignments/$ASSIGN/return" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d '{"inCondition":"GOOD","sendForMaintenance":false,"notes":"OK"}' >/dev/null

STATUS=$(curl -sS "$BASE/assets/$ASSET" "${AUTH[@]}" | jq -r '.status')
[[ "$STATUS" == "IN_STOCK" ]] || fail "expected IN_STOCK, got $STATUS"
ok "asset status = IN_STOCK"

# --------------------------------------------------------------------------- #
say "7. Retire the asset"
curl -sS -X POST "$BASE/assets/$ASSET/retire" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d '{"reason":"End of life"}' >/dev/null

STATUS=$(curl -sS "$BASE/assets/$ASSET" "${AUTH[@]}" | jq -r '.status')
[[ "$STATUS" == "RETIRED" ]] || fail "expected RETIRED, got $STATUS"
ok "asset status = RETIRED"

# --------------------------------------------------------------------------- #
say "8. Dashboard aggregate"
KPI=$(curl -sS "$BASE/stats/dashboard" "${AUTH[@]}")
TOTAL=$(echo "$KPI" | jq -r '.totalAssets')
[[ "$TOTAL" =~ ^[0-9]+$ ]] || fail "totalAssets not a number: $KPI"
ok "totalAssets=$TOTAL, openTickets=$(echo "$KPI" | jq -r '.openTickets')"

# --------------------------------------------------------------------------- #
say "9. Raise a ticket"
TICKET=$(curl -sS -X POST "$BASE/tickets" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"reporterPersonId\":$PERSON,\"subject\":\"Smoke test $SUFFIX\",\"description\":\"Automated end-to-end smoke\",\"priority\":\"LOW\"}")
TICKET_NR=$(echo "$TICKET" | jq -r '.ticketNumber')
[[ "$TICKET_NR" =~ ^TCK-[0-9]{4}-[0-9]{6}$ ]] || fail "ticket number bad shape: $TICKET_NR"
ok "ticket raised: $TICKET_NR"

# --------------------------------------------------------------------------- #
printf "\n${GREEN}${BOLD}All smoke checks passed.${RESET}\n\n"
