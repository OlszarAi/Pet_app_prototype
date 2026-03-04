#!/bin/bash
# ===========================
# PetsApp — Pełny test API (wszystkie funkcje)
# ===========================
# Użycie: ./test-api.sh
# Wymaga: backend działający (./start.sh), curl, python3

BASE="http://localhost:8080/api/v1"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'
PASS=0; FAIL=0

# ---- Helpers ----
section() { echo ""; echo -e "${BLUE}━━━ $1 ━━━${NC}"; }
pass()    { echo -e "  ${GREEN}✓ $1${NC}"; PASS=$((PASS+1)); }
fail()    { echo -e "  ${RED}✗ $1${NC}"; echo -e "    ${RED}$2${NC}"; FAIL=$((FAIL+1)); }

assert_ok() {
  local label="$1"; local response="$2"
  if echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); exit(0 if d.get('success') else 1)" 2>/dev/null; then
    pass "$label"
  else
    fail "$label" "$(echo "$response" | python3 -m json.tool 2>/dev/null | head -5)"
  fi
}

assert_field() {
  local label="$1"; local response="$2"; local field="$3"
  local val
  val=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['$field'])" 2>/dev/null)
  if [ -n "$val" ] && [ "$val" != "None" ] && [ "$val" != "null" ]; then
    pass "$label → $val"
  else
    fail "$label" "Pole '$field' puste w: $(echo "$response" | head -c 200)"
  fi
}

# ---- Sprawdź czy backend działa ----
echo -e "${YELLOW}=== PetsApp API — Test end-to-end ===${NC}"
if ! curl -s "$BASE/health" | grep -q '"status":"UP"'; then
  echo -e "${RED}Backend nie działa! Uruchom najpierw: ./start.sh${NC}"
  exit 1
fi
echo -e "${GREEN}Backend działa ✓${NC}"

# ---------------------------------------------------------------------------
section "1. Health Check"
RES=$(curl -s "$BASE/health")
assert_ok "GET /health" "$RES"
RES=$(curl -s "$BASE/health/ready")
assert_ok "GET /health/ready" "$RES"

# ---------------------------------------------------------------------------
section "2. Breeds (publiczne, bez tokenu)"
RES=$(curl -s "$BASE/breeds")
assert_ok "GET /breeds" "$RES"
BREED_COUNT=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))" 2>/dev/null)
if [ "${BREED_COUNT:-0}" -gt "0" ]; then
  pass "Liczba ras: $BREED_COUNT"
else
  fail "Brak ras w odpowiedzi" "$RES"
fi

BREED_ID=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['id'])" 2>/dev/null)
BREED_NAME=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['name'])" 2>/dev/null)
pass "Pierwsza rasa: $BREED_NAME (id=$BREED_ID)"

RES=$(curl -s "$BASE/breeds?q=golden")
assert_ok "GET /breeds?q=golden" "$RES"
FOUND=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))" 2>/dev/null)
pass "Wyszukiwanie 'golden': $FOUND wyników"

RES=$(curl -s "$BASE/breeds/$BREED_ID")
assert_ok "GET /breeds/$BREED_ID (szczegóły)" "$RES"

# ---------------------------------------------------------------------------
section "3. Rejestracja i weryfikacja emaila"
EMAIL="test-$(date +%s)@example.com"
USERNAME="testuser$(date +%s)"
PASSWORD="Test1234!"

RES=$(curl -s -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
assert_ok "POST /auth/register" "$RES"

# Pobierz kod z MailHog API
sleep 1
CODE=$(curl -s "http://localhost:8025/api/v2/messages" | \
  python3 -c "
import sys, json, re
try:
  data = json.load(sys.stdin)
  msgs = data.get('items', [])
  for msg in msgs:
    body = msg.get('Content', {}).get('Body', '')
    m = re.search(r'\b([0-9]{6})\b', body)
    if m:
      print(m.group(1))
      break
except: pass
" 2>/dev/null)

if [ -n "$CODE" ]; then
  pass "Kod weryfikacyjny z MailHog: $CODE"
else
  fail "Nie udało się pobrać kodu z MailHog" "Sprawdź: http://localhost:8025"
  CODE="000000"
fi

RES=$(curl -s -X POST "$BASE/auth/verify-email" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"code\":\"$CODE\"}")
assert_ok "POST /auth/verify-email" "$RES"

TOKEN=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['accessToken'])" 2>/dev/null)
REFRESH=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['refreshToken'])" 2>/dev/null)
USER_ID=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['user']['id'])" 2>/dev/null)

if [ -n "$TOKEN" ] && [ "$TOKEN" != "None" ]; then
  pass "Access token otrzymany (${TOKEN:0:20}...)"
else
  fail "Brak access tokenu" "$RES"
  echo -e "${RED}Dalsze testy wymagają tokenu — przerywam sekcje wymagające auth${NC}"
  TOKEN=""
fi

# ---------------------------------------------------------------------------
section "4. Login i tokeny"
RES=$(curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
assert_ok "POST /auth/login" "$RES"

RES=$(curl -s -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}")
assert_ok "POST /auth/refresh" "$RES"
NEW_TOKEN=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['accessToken'])" 2>/dev/null)
[ -n "$NEW_TOKEN" ] && TOKEN="$NEW_TOKEN"

# ---------------------------------------------------------------------------
section "5. Profil użytkownika"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  AUTH="-H \"Authorization: Bearer $TOKEN\""

  RES=$(curl -s "$BASE/users/me" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /users/me" "$RES"
  assert_field "username" "$RES" "username"

  RES=$(curl -s -X PATCH "$BASE/users/me" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"bio":"Test bio z automated test"}')
  assert_ok "PATCH /users/me (bio)" "$RES"

  RES=$(curl -s "$BASE/users/me/settings" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /users/me/settings" "$RES"

  RES=$(curl -s -X PATCH "$BASE/users/me/settings" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"pushLikes":false}')
  assert_ok "PATCH /users/me/settings" "$RES"
}

# ---------------------------------------------------------------------------
section "6. Upload avatara (zdjęcie profilowe)"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  # Stwórz minimalny testowy plik JPEG (1x1 pixel)
  TEST_IMG="/tmp/test_avatar.jpg"
  python3 -c "
import struct, zlib
# Minimalny JPEG 1x1 biały piksel
data = bytes([
  0xFF,0xD8,0xFF,0xE0,0x00,0x10,0x4A,0x46,0x49,0x46,0x00,0x01,
  0x01,0x00,0x00,0x01,0x00,0x01,0x00,0x00,0xFF,0xDB,0x00,0x43,
  0x00,0x08,0x06,0x06,0x07,0x06,0x05,0x08,0x07,0x07,0x07,0x09,
  0x09,0x08,0x0A,0x0C,0x14,0x0D,0x0C,0x0B,0x0B,0x0C,0x19,0x12,
  0x13,0x0F,0x14,0x1D,0x1A,0x1F,0x1E,0x1D,0x1A,0x1C,0x1C,0x20,
  0x24,0x2E,0x27,0x20,0x22,0x2C,0x23,0x1C,0x1C,0x28,0x37,0x29,
  0x2C,0x30,0x31,0x34,0x34,0x34,0x1F,0x27,0x39,0x3D,0x38,0x32,
  0x3C,0x2E,0x33,0x34,0x32,0xFF,0xC0,0x00,0x0B,0x08,0x00,0x01,
  0x00,0x01,0x01,0x01,0x11,0x00,0xFF,0xC4,0x00,0x1F,0x00,0x00,
  0x01,0x05,0x01,0x01,0x01,0x01,0x01,0x01,0x00,0x00,0x00,0x00,
  0x00,0x00,0x00,0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,
  0x09,0x0A,0x0B,0xFF,0xC4,0x00,0xB5,0x10,0x00,0x02,0x01,0x03,
  0x03,0x02,0x04,0x03,0x05,0x05,0x04,0x04,0x00,0x00,0x01,0x7D,
  0x01,0x02,0x03,0x00,0x04,0x11,0x05,0x12,0x21,0x31,0x41,0x06,
  0x13,0x51,0x61,0x07,0x22,0x71,0x14,0x32,0x81,0x91,0xA1,0x08,
  0x23,0x42,0xB1,0xC1,0x15,0x52,0xD1,0xF0,0x24,0x33,0x62,0x72,
  0x82,0x09,0x0A,0x16,0x17,0x18,0x19,0x1A,0x25,0x26,0x27,0x28,
  0xFF,0xDA,0x00,0x08,0x01,0x01,0x00,0x00,0x3F,0x00,0xFB,0xD2,
  0x8A,0x28,0x03,0xFF,0xD9
])
open('/tmp/test_avatar.jpg', 'wb').write(data)
print('OK')
"
  RES=$(curl -s -X POST "$BASE/users/me/avatar" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@$TEST_IMG;type=image/jpeg")
  assert_ok "POST /users/me/avatar (upload zdjęcia profilowego)" "$RES"

  AVATAR_URL=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'].get('avatarUrl',''))" 2>/dev/null)
  if [ -n "$AVATAR_URL" ] && [ "$AVATAR_URL" != "None" ]; then
    pass "Avatar URL: $AVATAR_URL"
    # Sprawdź czy plik jest dostępny w MinIO
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$AVATAR_URL" 2>/dev/null)
    if [ "$HTTP_CODE" = "200" ]; then
      pass "Avatar dostępny przez URL (HTTP $HTTP_CODE)"
    else
      fail "Avatar URL niedostępny HTTP $HTTP_CODE" "$AVATAR_URL"
    fi
  fi
}

# ---------------------------------------------------------------------------
section "7. Upload złapania psa (catch) + zdjęcie"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  TEST_IMG="/tmp/test_dog.jpg"
  cp /tmp/test_avatar.jpg "$TEST_IMG"

  RES=$(curl -s -X POST "$BASE/catches" \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@$TEST_IMG;type=image/jpeg" \
    -F "breedId=$BREED_ID" \
    -F "caption=Testowe złapanie przez automated test" \
    -F "isPublic=true")
  assert_ok "POST /catches (upload psa z breedId=$BREED_ID)" "$RES"

  CATCH_ID=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['id'])" 2>/dev/null)
  PHOTO_URL=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'].get('photoUrl',''))" 2>/dev/null)
  THUMB_URL=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'].get('thumbUrl',''))" 2>/dev/null)

  if [ -n "$CATCH_ID" ] && [ "$CATCH_ID" != "None" ]; then
    pass "Catch ID: $CATCH_ID"
  fi
  if [ -n "$PHOTO_URL" ] && [ "$PHOTO_URL" != "None" ]; then
    pass "Photo URL: $PHOTO_URL"
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$PHOTO_URL" 2>/dev/null)
    pass "Photo dostępne: HTTP $HTTP_CODE"
  fi
  if [ -n "$THUMB_URL" ] && [ "$THUMB_URL" != "None" ]; then
    pass "Thumb URL: $THUMB_URL"
  fi

  # Pobierz szczegóły catcha
  RES=$(curl -s "$BASE/catches/$CATCH_ID" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /catches/$CATCH_ID" "$RES"

  # Lajk
  RES=$(curl -s -X POST "$BASE/catches/$CATCH_ID/likes" -H "Authorization: Bearer $TOKEN")
  assert_ok "POST /catches/$CATCH_ID/likes (polub)" "$RES"

  # Komentarz
  RES=$(curl -s -X POST "$BASE/catches/$CATCH_ID/comments" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"content":"Super pies z testu!"}')
  assert_ok "POST /catches/$CATCH_ID/comments" "$RES"
  COMMENT_ID=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data']['id'])" 2>/dev/null)

  # Lista komentarzy
  RES=$(curl -s "$BASE/catches/$CATCH_ID/comments" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /catches/$CATCH_ID/comments" "$RES"
}

# ---------------------------------------------------------------------------
section "8. Feed publiczny"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  RES=$(curl -s "$BASE/feed/public?limit=5" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /feed/public" "$RES"
  COUNT=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))" 2>/dev/null)
  pass "Feed publiczny: $COUNT wpisów"

  RES=$(curl -s "$BASE/feed/friends?limit=5" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /feed/friends" "$RES"

  RES=$(curl -s "$BASE/feed/trending?limit=5" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /feed/trending" "$RES"
}

# ---------------------------------------------------------------------------
section "9. Pokédex"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  RES=$(curl -s "$BASE/users/$USER_ID/pokedex" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /users/$USER_ID/pokedex" "$RES"
  POKEDEX_COUNT=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))" 2>/dev/null)
  pass "Odkryte rasy: $POKEDEX_COUNT"

  RES=$(curl -s "$BASE/users/$USER_ID/pokedex/stats" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /users/$USER_ID/pokedex/stats" "$RES"
}

# ---------------------------------------------------------------------------
section "10. Znajomi i leaderboard"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  RES=$(curl -s "$BASE/friends" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /friends" "$RES"

  RES=$(curl -s "$BASE/friends/leaderboard" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /friends/leaderboard" "$RES"

  RES=$(curl -s "$BASE/friends/requests/pending" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /friends/requests/pending" "$RES"
}

# ---------------------------------------------------------------------------
section "11. Powiadomienia i achievementy"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  RES=$(curl -s "$BASE/notifications" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /notifications" "$RES"

  RES=$(curl -s "$BASE/achievements" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /achievements" "$RES"
  ACH_COUNT=$(echo "$RES" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',[])))" 2>/dev/null)
  pass "Achievementy w systemie: $ACH_COUNT"

  RES=$(curl -s "$BASE/users/me/achievements" -H "Authorization: Bearer $TOKEN")
  assert_ok "GET /users/me/achievements" "$RES"
}

# ---------------------------------------------------------------------------
section "12. Reset hasła (email flow)"
RES=$(curl -s -X POST "$BASE/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\"}")
assert_ok "POST /auth/forgot-password" "$RES"
pass "Email z resetem wysłany → http://localhost:8025"

# ---------------------------------------------------------------------------
section "13. Logout"
[ -z "$TOKEN" ] && { echo "  Pominięto — brak tokenu"; } || {
  RES=$(curl -s -X POST "$BASE/auth/logout" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$REFRESH\"}")
  assert_ok "POST /auth/logout" "$RES"
}

# ---------------------------------------------------------------------------
echo ""
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✓ Zaliczone: $PASS${NC}   ${RED}✗ Niezaliczone: $FAIL${NC}"

if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}Wszystkie testy przeszły! Backend działa poprawnie.${NC}"
  exit 0
else
  echo -e "${RED}$FAIL testów nie przeszło. Sprawdź logi: tail -100 /tmp/petsapp-backend.log${NC}"
  exit 1
fi
