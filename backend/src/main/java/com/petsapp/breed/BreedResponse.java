package com.petsapp.breed;

/**
 * DTO odpowiedzi dla endpointu listy ras ({@code GET /breeds}).
 *
 * <p>Nie zawiera globalnych statystyk catchow — te sa tylko w {@link BreedDetailResponse} (GET
 * /breeds/:id) aby uniknac N+1 na liscie.
 */
public record BreedResponse(
    int id,
    String name,
    String namePl,
    String group,
    String sizeCategory,
    String description,
    String silhouetteUrl,
    short rarityScore) {

  /** Mapuje encje na DTO. */
  public static BreedResponse from(Breed breed) {
    return new BreedResponse(
        breed.getId(),
        breed.getName(),
        breed.getNamePl(),
        breed.getGroup(),
        breed.getSizeCategory(),
        breed.getDescription(),
        breed.getSilhouetteUrl(),
        breed.getRarityScore());
  }
}
