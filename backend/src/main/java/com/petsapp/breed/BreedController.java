package com.petsapp.breed;

import com.petsapp.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy zarzadzania rasami psow.
 *
 * <p>Wszystkie endpointy sa publiczne (bez JWT). Dane ras sa tylko do odczytu — seed jest
 * zarzadzany przez Flyway.
 */
@RestController
@RequestMapping("/breeds")
public class BreedController {

  private final BreedService breedService;

  public BreedController(BreedService breedService) {
    this.breedService = breedService;
  }

  /**
   * Lista aktywnych ras z opcjonalnym filtrowaniem.
   *
   * @param q fragment nazwy (EN lub PL), case-insensitive
   * @param group skupina AKC/FCI np. "Herding", "Hound"
   * @param size kategoria rozmiaru: "small", "medium" lub "large"
   */
  @GetMapping
  public ResponseEntity<ApiResponse<List<BreedResponse>>> getBreeds(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String group,
      @RequestParam(name = "size", required = false)
          @Pattern(
              regexp = "^(small|medium|large)$",
              message = "size must be one of: small, medium, large")
          String size) {
    List<BreedResponse> breeds = breedService.getBreeds(q, group, size);
    return ResponseEntity.ok(ApiResponse.ok(breeds));
  }

  /**
   * Szczegoly rasy wraz z globalnymi statystykami (ilu uzytkownikow zlowilo ta rase).
   *
   * @param id ID rasy (liczba calkowita)
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<BreedDetailResponse>> getBreedById(
      @PathVariable @Max(value = 1_000_000, message = "invalid breed id") int id) {
    BreedDetailResponse detail = breedService.getBreedById(id);
    return ResponseEntity.ok(ApiResponse.ok(detail));
  }
}
