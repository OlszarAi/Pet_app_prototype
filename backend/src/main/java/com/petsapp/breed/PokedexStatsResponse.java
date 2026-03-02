package com.petsapp.breed;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO ze statystykami Pokedeksu uzytkownika. Uzywane w odpowiedzi {@code GET
 * /users/:id/pokedex/stats}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PokedexStatsResponse(
    long uniqueBreeds,
    long totalActiveBreeds,
    double progressPercent,
    long totalCatches,
    int currentStreak,
    BreedResponse favoriteBreed) {}
