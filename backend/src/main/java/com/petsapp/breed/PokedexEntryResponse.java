package com.petsapp.breed;

/**
 * DTO reprezentujace pojedynczy wpis w pokdeksie — rase wraz z liczba zlowien przez danego
 * uzytkownika. Uzywane w odpowiedzi {@code GET /users/:id/pokedex}.
 */
public record PokedexEntryResponse(
    int breedId,
    String name,
    String namePl,
    String group,
    String sizeCategory,
    String silhouetteUrl,
    short rarityScore,
    long catchCount) {

  /** Tworzy wpis na podstawie encji Breed i liczby zlowien. */
  public static PokedexEntryResponse from(Breed breed, long catchCount) {
    return new PokedexEntryResponse(
        breed.getId(),
        breed.getName(),
        breed.getNamePl(),
        breed.getGroup(),
        breed.getSizeCategory(),
        breed.getSilhouetteUrl(),
        breed.getRarityScore(),
        catchCount);
  }
}
