#!/bin/bash
# ===========================
# PetsApp — Sprawdź status środowiska
# ===========================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()  { echo -e "  ${GREEN}✓ $1${NC}"; }
fail(){ echo -e "  ${RED}✗ $1${NC}"; }
warn(){ echo -e "  ${YELLOW}? $1${NC}"; }

echo ""
echo -e "${YELLOW}=== Status PetsApp ===${NC}"
echo ""

# Backend
if curl -s http://localhost:8080/api/v1/health | grep -q '"status":"UP"'; then
  ok "Backend działa     → http://localhost:8080/api/v1"
  ok "Swagger UI         → http://localhost:8080/api/v1/swagger-ui.html"
else
  fail "Backend NIE działa (uruchom: ./start.sh)"
fi

# PostgreSQL
if docker exec petsapp_postgres pg_isready -U petsapp -d petsapp_dev -q 2>/dev/null; then
  BREED_COUNT=$(docker exec petsapp_postgres psql -U petsapp -d petsapp_dev -tAc "SELECT COUNT(*) FROM breed" 2>/dev/null || echo "?")
  ok "PostgreSQL działa  → petsapp_dev ($BREED_COUNT ras w bazie)"
else
  fail "PostgreSQL NIE działa"
fi

# Redis
if docker exec petsapp_redis redis-cli ping 2>/dev/null | grep -q PONG; then
  ok "Redis działa       → localhost:6379"
else
  fail "Redis NIE działa"
fi

# MailHog
if curl -s http://localhost:8025 > /dev/null 2>&1; then
  ok "MailHog działa     → http://localhost:8025"
else
  fail "MailHog NIE działa"
fi

# MinIO
if curl -s http://localhost:9000/minio/health/live > /dev/null 2>&1; then
  ok "MinIO działa       → http://localhost:9001 (console)"
else
  fail "MinIO NIE działa"
fi

echo ""

# Sprawdź logi backendu jeśli istnieją
if [ -f /tmp/petsapp-backend.log ]; then
  ERRORS=$(grep -c "ERROR\|Exception\|FAILED" /tmp/petsapp-backend.log 2>/dev/null || echo 0)
  if [ "$ERRORS" -gt "0" ]; then
    warn "W logach backendu jest $ERRORS błędów → tail -50 /tmp/petsapp-backend.log"
  fi
fi
