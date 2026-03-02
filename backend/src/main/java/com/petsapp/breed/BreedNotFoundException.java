package com.petsapp.breed;

/**
 * Wyjatek rzucany gdy rasa o podanym ID nie istnieje lub nie jest aktywna.
 *
 * <p>Mapowany na HTTP 404 przez {@code GlobalExceptionHandler}.
 */
public class BreedNotFoundException extends RuntimeException {

  private final int breedId;

  public BreedNotFoundException(int breedId) {
    super("Breed not found: " + breedId);
    this.breedId = breedId;
  }

  public int getBreedId() {
    return breedId;
  }
}
