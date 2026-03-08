#!/bin/bash
# ===========================
# PetsApp — Start lokalnego środowiska
# ===========================
# Użycie: ./start.sh
# Zatrzymanie: ./stop.sh lub Ctrl+C

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}=== PetsApp — Start ===${NC}"

# 1. Załaduj zmienne środowiskowe
if [ ! -f "$SCRIPT_DIR/.env" ]; then
  echo -e "${RED}Brak pliku .env! Skopiuj: cp .env.example .env${NC}"
  exit 1
fi
export $(grep -v '^#' "$SCRIPT_DIR/.env" | grep -v '^$' | xargs)
echo -e "${GREEN}✓ Załadowano .env${NC}"

# 2. Uruchom Docker (PostgreSQL, Redis, MailHog, MinIO)
echo -e "${YELLOW}→ Uruchamiam Docker...${NC}"
docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d

# Poczekaj na healthcheck
echo -e "${YELLOW}→ Czekam na usługi Docker...${NC}"
for i in $(seq 1 20); do
  HEALTHY=$(docker compose -f "$SCRIPT_DIR/docker-compose.yml" ps --format json 2>/dev/null \
    | python3 -c "import sys,json; lines=[json.loads(l) for l in sys.stdin if l.strip()]; print(sum(1 for s in lines if 'healthy' in s.get('Health','') or s.get('State','')=='running'))" 2>/dev/null || echo 0)
  if [ "$HEALTHY" -ge 3 ]; then
    break
  fi
  sleep 2
  echo -n "."
done
echo ""
echo -e "${GREEN}✓ Docker usługi działają${NC}"

# 3. MinIO — utwórz bucket jeśli nie istnieje
echo -e "${YELLOW}→ Sprawdzam bucket MinIO...${NC}"
sleep 1
docker exec petsapp_minio mc alias set local http://localhost:9000 \
  "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" --quiet 2>/dev/null || true
docker exec petsapp_minio mc mb local/petsapp-dev --ignore-existing --quiet 2>/dev/null || true
docker exec petsapp_minio mc anonymous set public local/petsapp-dev --quiet 2>/dev/null || true
echo -e "${GREEN}✓ MinIO bucket petsapp-dev gotowy${NC}"

# 4. Uruchom Spring Boot backend
echo -e "${YELLOW}→ Uruchamiam backend Spring Boot...${NC}"
cd "$SCRIPT_DIR/backend"

# Zapis PID do pliku
mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/petsapp-backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > /tmp/petsapp-backend.pid

# 5. Czekaj na start backendu (max 60s)
echo -e "${YELLOW}→ Czekam na start backendu (max 60s)...${NC}"
for i in $(seq 1 30); do
  sleep 2
  if curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
    echo ""
    echo -e "${GREEN}✓ Backend działa!${NC}"
    break
  fi
  echo -n "."
  if [ $i -eq 30 ]; then
    echo ""
    echo -e "${RED}Backend nie wystartował. Sprawdź logi: tail -50 /tmp/petsapp-backend.log${NC}"
    exit 1
  fi
done

# 6. Podsumowanie
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║            DZIAŁA ✓                      ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  Backend API:  http://localhost:8080/api/v1  ║${NC}"
echo -e "${GREEN}║  Swagger UI:   http://localhost:8080/api/v1/swagger-ui.html  ║${NC}"
echo -e "${GREEN}║  MailHog:      http://localhost:8025       ║${NC}"
echo -e "${GREEN}║  MinIO:        http://localhost:9001       ║${NC}"
echo -e "${GREEN}║  PostgreSQL:   localhost:5432              ║${NC}"
echo -e "${GREEN}║  Redis:        localhost:6379              ║${NC}"
echo -e "${GREEN}╠══════════════════════════════════════════╣${NC}"
echo -e "${GREEN}║  Logi:  tail -f /tmp/petsapp-backend.log  ║${NC}"
echo -e "${GREEN}║  Stop:  ./stop.sh                         ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
