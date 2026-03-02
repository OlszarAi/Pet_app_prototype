-- Migracja V5: konwersja kolumny friendship.status z natywnego typu PostgreSQL enum
-- do VARCHAR(20). Zmiana jest konieczna poniewaz Hibernate @Enumerated(EnumType.STRING)
-- operuje na wartosciach uppercase (PENDING/ACCEPTED/BLOCKED), a natywny typ PostgreSQL
-- mial zdefiniowane wartosci lowercase (pending/accepted/blocked). Dodatkowo, Hibernate 6
-- generuje niekompatybilne rzutowanie typow (FriendshipStatus zamiast friendship_status).
--
-- Po migracji: Java enum jest mapowany 1:1 na VARCHAR, constraint CHECK zapewnia integralnosc.

-- Krok 1: Zmien typ kolumny na VARCHAR(20) i jednoczesnie konwertuj wartosci do uppercase
-- (USING clause jest wykonywany dla kazdego wiersza podczas strukturalnej zmiany kolumny)
ALTER TABLE friendship
    ALTER COLUMN status TYPE VARCHAR(20)
    USING upper(status::text);

-- Krok 2: Ustaw nowy DEFAULT zgodny z Java enum
ALTER TABLE friendship
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- Krok 3: Dodaj CHECK constraint dla integralnosci danych
ALTER TABLE friendship
    ADD CONSTRAINT friendship_status_check
    CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED'));

-- Krok 4: Usun nieuzywany juz natywny typ enum
DROP TYPE friendship_status;
