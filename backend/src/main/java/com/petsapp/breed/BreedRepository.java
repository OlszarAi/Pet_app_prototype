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
   * <p>Parametry null sa ignorowane — metoda dziala jak dynamic query bez Specification API dzieki
   * jawnym warunkom CASE WHEN.
   */
  @Query(
      """
      SELECT b FROM Breed b
      WHERE b.isActive = true
        AND (:q IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(b.namePl) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:group IS NULL OR b.group = :group)
        AND (:sizeCategory IS NULL OR b.sizeCategory = :sizeCategory)
      ORDER BY b.rarityScore ASC, b.name ASC
      """)
  List<Breed> findAllByFilter(
      @Param("q") String q,
      @Param("group") String group,
      @Param("sizeCategory") String sizeCategory);

  /** Zwraca wszystkie unikalne grupy aktywnych ras — przydatne do budowania filtrow w UI. */
  @Query(
      "SELECT DISTINCT b.group FROM Breed b WHERE b.isActive = true AND b.group IS NOT NULL ORDER BY b.group ASC")
  List<String> findAllActiveGroups();

  /** Zwraca liczbe aktywnych ras — uzywana w statystykach pokedex. */
  @Query("SELECT COUNT(b) FROM Breed b WHERE b.isActive = true")
  long countActiveBreeds();
}
