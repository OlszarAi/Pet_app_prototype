package com.petsapp.catch_;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium dla encji {@link CatchReport}.
 *
 * <p>Zapis-only w biezacej implementacji — zgloszenia sa przetwarzane recznie przez adminow.
 */
public interface CatchReportRepository extends JpaRepository<CatchReport, UUID> {}
