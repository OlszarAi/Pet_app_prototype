package com.petsapp.breed;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link Breed}.
 *
 * <p>Rasy sa tylko do odczytu z perspektywy API — zarzadza nimi Flyway seed. Metody filtrowania
 * uzywaja JPQL z LOWER() dla case-insensitive search.
 */
public interface BreedRepository extends JpaRepository<Breed, Integer> {

  /**
   * Wyszukuje aktywne rasy z opcjonalnym filtrowaniem po nazwie, grupie i rozmiarze.
   *
   * <p>Uzywa natywnego SQL z CAST(:param AS TEXT) — rozwiazuje blad PostgreSQL
   * "function lower(bytea) does not exist" gdy JDBC przekazuje null bez informacji o typie.
   */
  @Query(
      value =
          """
          SELECT * FROM breed
          WHERE is_active = true
            AND (CAST(:q AS TEXT) IS NULL
                 OR LOWER(name) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(name_pl) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (CAST(:grp AS TEXT) IS NULL OR "group" = CAST(:grp AS TEXT))
            AND (CAST(:sizeCategory AS TEXT) IS NULL OR size_category = CAST(:sizeCategory AS TEXT))
          ORDER BY rarity_score ASC, name ASC
          """,
      nativeQuery = true)
  List<Breed> findAllByFilter(
      @Param("q") String q,
      @Param("grp") String group,
      @Param("sizeCategory") String sizeCategory);

  /** Zwraca wszystkie unikalne grupy aktywnych ras — przydatne do budowania filtrow w UI. */
  @Query(
      "SELECT DISTINCT b.group FROM Breed b WHERE b.isActive = true AND b.group IS NOT NULL ORDER BY b.group ASC")
  List<String> findAllActiveGroups();

  /** Zwraca liczbe aktywnych ras — uzywana w statystykach pokedex. */
  @Query("SELECT COUNT(b) FROM Breed b WHERE b.isActive = true")
  long countActiveBreeds();
}
