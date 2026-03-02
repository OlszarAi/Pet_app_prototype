package com.petsapp.breed;

/**
 * DTO odpowiedzi dla endpointu szczegolów rasy ({@code GET /breeds/:id}).
 *
 * <p>Rozszerza podstawowe dane rasy o globalne statystyki — ile unikalnych uzytkownikow zlowilo te
 * rase. Statystyki sa liczone live, mozna je pozniej skalowac przez cache.
 */
public record BreedDetailResponse(
    int id,
    String name,
    String namePl,
    String group,
    String sizeCategory,
    String description,
    String silhouetteUrl,
    short rarityScore,
    long totalCatchers,
    long totalCatches) {

  /** Mapuje encje i statystyki na DTO. */
  public static BreedDetailResponse from(Breed breed, long totalCatchers, long totalCatches) {
    return new BreedDetailResponse(
        breed.getId(),
        breed.getName(),
        breed.getNamePl(),
        breed.getGroup(),
        breed.getSizeCategory(),
        breed.getDescription(),
        breed.getSilhouetteUrl(),
        breed.getRarityScore(),
        totalCatchers,
        totalCatches);
  }
}
