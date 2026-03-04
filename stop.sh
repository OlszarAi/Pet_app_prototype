#!/bin/bash
# ===========================
# PetsApp — Stop lokalnego środowiska
# ===========================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}=== PetsApp — Stop ===${NC}"

# 1. Zatrzymaj backend Spring Boot
if [ -f /tmp/petsapp-backend.pid ]; then
  PID=$(cat /tmp/petsapp-backend.pid)
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    echo -e "${GREEN}✓ Backend zatrzymany (PID $PID)${NC}"
  else
    echo "Backend już nie działał"
  fi
  rm -f /tmp/petsapp-backend.pid
else
  # Fallback — kill po nazwie procesu
  pkill -f "petsapp-backend" 2>/dev/null && echo -e "${GREEN}✓ Backend zatrzymany${NC}" || true
  pkill -f "spring-boot:run" 2>/dev/null || true
fi

# 2. Zatrzymaj Docker (zachowuje dane)
echo -e "${YELLOW}→ Zatrzymuję Docker...${NC}"
docker compose -f "$SCRIPT_DIR/docker-compose.yml" stop
echo -e "${GREEN}✓ Docker zatrzymany (dane zachowane)${NC}"

echo ""
echo -e "${GREEN}Środowisko zatrzymane.${NC}"
echo "Aby usunąć dane bazy: docker compose -f docker-compose.yml down -v"
