package com.petsapp.breed;

import com.petsapp.auth.User;
import com.petsapp.common.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy Pokedeksu uzytkownika — lista odkrytych ras i statystyki.
 *
 * <p>Kontroler jest osobny od {@code BreedController} aby zachowac zasade jednej odpowiedzialnosci:
 * ten kontroler dotyczy danych uzytkownika, tamten — katalogu ras. Oba wymagaja autowire {@link
 * BreedService}. Dostep do pokedeksu prywatnego profilu jest mozliwy tylko przez wlasciciela —
 * weryfikowane w serwisie.
 */
@RestController
@RequestMapping("/users")
public class PokedexController {

  private final BreedService breedService;

  public PokedexController(BreedService breedService) {
    this.breedService = breedService;
  }

  /**
   * Zwraca liste ras odkrytych przez uzytkownika (posortowanych malejaco po liczbie zlowien).
   *
   * @param userId ID uzytkownika, ktorego Pokedex chcemy obejrzec
   * @param currentUser zalogowany uzytkownik (null jezeli request jest bez tokena — niemozliwe
   *     poniewaz endpoint jest chroniony przez SecurityConfig)
   */
  @GetMapping("/{userId}/pokedex")
  public ResponseEntity<ApiResponse<List<PokedexEntryResponse>>> getPokedex(
      @PathVariable UUID userId, @AuthenticationPrincipal User currentUser) {
    List<PokedexEntryResponse> entries = breedService.getPokedex(userId, currentUser.getId());
    return ResponseEntity.ok(ApiResponse.ok(entries));
  }

  /**
   * Zwraca statystyki Pokedeksu: postep, streak, ulubiona rasa, totale.
   *
   * @param userId ID uzytkownika, ktorego statystyki chcemy obejrzec
   * @param currentUser zalogowany uzytkownik
   */
  @GetMapping("/{userId}/pokedex/stats")
  public ResponseEntity<ApiResponse<PokedexStatsResponse>> getPokedexStats(
      @PathVariable UUID userId, @AuthenticationPrincipal User currentUser) {
    PokedexStatsResponse stats = breedService.getPokedexStats(userId, currentUser.getId());
    return ResponseEntity.ok(ApiResponse.ok(stats));
  }
}
