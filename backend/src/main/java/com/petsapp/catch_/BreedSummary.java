package com.petsapp.catch_;

import com.petsapp.breed.Breed;

/**
 * Minimalne dane rasy osadzane w CatchResponse.
 *
 * <p>Zamiast pelnej encji Breed (description, silhouetteUrl itp.) zwracamy tylko to,
 * co jest potrzebne do wyswietlenia karty polowania w feedzie.
 */
public record BreedSummary(int id, String name, String namePl, short rarityScore) {

  public static BreedSummary from(Breed breed) {
    return new BreedSummary(breed.getId(), breed.getName(), breed.getNamePl(), breed.getRarityScore());
  }
}
