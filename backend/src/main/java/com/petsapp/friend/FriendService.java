package com.petsapp.friend;

import com.petsapp.common.ConflictException;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.common.ApiResponse;
import com.petsapp.notification.NotificationService;
import com.petsapp.user.UserNotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logika biznesowa dla modulu znajomych.
 *
 * <p>Obsluguje: wysylanie/akceptowanie/odrzucanie/usuwanie prosb, blokowanie oraz
 * ranking znajomych (leaderboard z cache Redis TTL=1h).
 */
@Service
public class FriendService {

  private static final Logger log = LoggerFactory.getLogger(FriendService.class);
  private static final String LEADERBOARD_CACHE_PREFIX = "leaderboard:";

  private final FriendshipRepository friendshipRepository;
  private final UserRepository userRepository;
  private final StringRedisTemplate redisTemplate;
  private final NotificationService notificationService;

  public FriendService(
      FriendshipRepository friendshipRepository,
      UserRepository userRepository,
      StringRedisTemplate redisTemplate,
      NotificationService notificationService) {
    this.friendshipRepository = friendshipRepository;
    this.userRepository = userRepository;
    this.redisTemplate = redisTemplate;
    this.notificationService = notificationService;
  }

  /**
   * Wysyla prosbe o znajomosc.
   *
   * <p>Rzuca {@link ConflictException} gdy relacja juz istnieje (oczekujaca, zaakceptowana lub
   * zablokowana).
   *
   * @param requester zalogowany uzytkownik
   * @param targetUserId ID uzytkownika do ktorego wysylamy prosbe
   * @return DTO nowej relacji
   */
  @Transactional
  public FriendResponse sendRequest(User requester, UUID targetUserId) {
    if (requester.getId().equals(targetUserId)) {
      throw new ConflictException("Cannot send friend request to yourself");
    }

    User target = findActiveUser(targetUserId);

    friendshipRepository
        .findBetween(requester.getId(), targetUserId)
        .ifPresent(
            existing -> {
              throw new ConflictException(
                  "Friendship already exists with status: " + existing.getStatus().name());
            });

    Friendship friendship = Friendship.pending(requester, target);
    Friendship saved = friendshipRepository.save(friendship);

    // Powiadom addressee asynchronicznie
    notificationService.notifyFriendRequest(target, requester.getUsername(), saved.getId());

    log.debug(
        "Friend request sent: requesterId={}, targetId={}", requester.getId(), targetUserId);
    return FriendResponse.from(saved, requester.getId());
  }

  /**
   * Akceptuje oczekujaca prosbe o znajomosc.
   *
   * <p>Tylko addressee moze akceptowac prosbe (nie requester).
   *
   * @param currentUser zalogowany uzytkownik (musi byc addressee)
   * @param friendshipId ID relacji do zaakceptowania
   * @return DTO zaakceptowanej relacji
   */
  @Transactional
  public FriendResponse acceptRequest(User currentUser, UUID friendshipId) {
    Friendship friendship = findPendingRequestForUser(friendshipId, currentUser.getId());
    friendship.accept();
    friendshipRepository.save(friendship);
    invalidateLeaderboardCache(currentUser.getId());

    // Powiadom requestera o akceptacji asynchronicznie
    notificationService.notifyFriendAccepted(
        friendship.getRequester(), currentUser.getUsername());

    log.debug(
        "Friend request accepted: friendshipId={}, userId={}", friendshipId, currentUser.getId());
    return FriendResponse.from(friendship, currentUser.getId());
  }

  /**
   * Odrzuca oczekujaca prosbe o znajomosc i usuwa ja z bazy.
   *
   * @param currentUser zalogowany uzytkownik (musi byc addressee)
   * @param friendshipId ID relacji do odrzucenia
   */
  @Transactional
  public void rejectRequest(User currentUser, UUID friendshipId) {
    Friendship friendship = findPendingRequestForUser(friendshipId, currentUser.getId());
    friendshipRepository.delete(friendship);

    log.debug(
        "Friend request rejected: friendshipId={}, userId={}", friendshipId, currentUser.getId());
  }

  /**
   * Usuwa istniejaca znajomosc (unfriend).
   *
   * <p>Kazda ze stron moze usunac znajomosc.
   *
   * @param currentUser zalogowany uzytkownik
   * @param targetUserId ID uzytkownika do usuniecia z znajomych
   */
  @Transactional
  public void removeFriend(User currentUser, UUID targetUserId) {
    Friendship friendship =
        friendshipRepository
            .findBetween(currentUser.getId(), targetUserId)
            .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
            .orElseThrow(
                () -> new FriendshipNotFoundException("No accepted friendship with user: " + targetUserId));
    friendshipRepository.delete(friendship);
    invalidateLeaderboardCache(currentUser.getId());

    log.debug(
        "Friendship removed: userId={}, targetId={}", currentUser.getId(), targetUserId);
  }

  /**
   * Blokuje uzytkownika.
   *
   * <p>Jesli istnieje poprzednia relacja (pending, accepted) — jest usuwana i tworzona nowa
   * BLOCKED. Zablokowany uzytkownik nie moze wysylac prosb i nie widzi profilu.
   *
   * @param currentUser zalogowany uzytkownik (blokujacy)
   * @param targetUserId ID uzytkownika do zablokowania
   */
  @Transactional
  public void blockUser(User currentUser, UUID targetUserId) {
    if (currentUser.getId().equals(targetUserId)) {
      throw new ConflictException("Cannot block yourself");
    }

    User target = findActiveUser(targetUserId);

    friendshipRepository
        .findBetween(currentUser.getId(), targetUserId)
        .ifPresent(existing -> {
          friendshipRepository.delete(existing);
          friendshipRepository.flush(); // Konieczne aby DELETE byl wykonany przed INSERT z tym samym kluczem
        });

    Friendship blocked = Friendship.blocked(currentUser, target);
    friendshipRepository.save(blocked);
    invalidateLeaderboardCache(currentUser.getId());

    log.debug("User blocked: blockerId={}, targetId={}", currentUser.getId(), targetUserId);
  }

  /**
   * Zwraca liste oczekujacych prosb o znajomosc dla biezacego uzytkownika.
   *
   * @param currentUser zalogowany uzytkownik
   * @return lista DTO prosb
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<FriendResponse>> getPendingRequests(User currentUser) {
    List<FriendResponse> requests =
        friendshipRepository.findPendingRequestsFor(currentUser.getId()).stream()
            .map(FriendResponse::fromPendingRequest)
            .toList();
    return ApiResponse.ok(requests);
  }

  /**
   * Zwraca liste zaakceptowanych znajomych uzytkownika.
   *
   * @param currentUser zalogowany uzytkownik
   * @return lista DTO znajomych
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<FriendResponse>> getFriends(User currentUser) {
    List<FriendResponse> friends =
        friendshipRepository.findAcceptedFriendships(currentUser.getId()).stream()
            .map(f -> FriendResponse.from(f, currentUser.getId()))
            .toList();
    return ApiResponse.ok(friends);
  }

  /**
   * Zwraca leaderboard znajomych posortowany malejaco po unique_breeds.
   *
   * <p>Wynik jest cachowany w Redis na 1h. Po wyjeciem z cache wywolujemy zapytanie DB.
   * Biezacy uzytkownik jest dolaczany na pozycji wynikowej z wlasnych statystyk.
   *
   * @param currentUser zalogowany uzytkownik
   * @return lista DTO pozycji rankingowych
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<LeaderboardEntryResponse>> getLeaderboard(User currentUser) {
    // Uzywamy findAcceptedFriendships zamiast JPQL CASE entity query
    // (Hibernate 6 nie wspiera prawidlowo CASE zwracajacego encje w JPQL)
    List<User> friends =
        friendshipRepository.findAcceptedFriendships(currentUser.getId()).stream()
            .map(
                f ->
                    f.getRequester().getId().equals(currentUser.getId())
                        ? f.getAddressee()
                        : f.getRequester())
            .toList();

    // Wstaw biezacego uzytkownika do rankingu i posortuj
    List<User> all = new java.util.ArrayList<>(friends);
    all.add(currentUser);
    all.sort((a, b) -> Integer.compare(b.getUniqueBreeds(), a.getUniqueBreeds()));

    List<LeaderboardEntryResponse> entries =
        IntStream.range(0, all.size())
            .mapToObj(i -> LeaderboardEntryResponse.from(all.get(i), i + 1))
            .toList();

    return ApiResponse.ok(entries);
  }

  // --- private helpers ---

  private User findActiveUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(u -> u.getDeletedAt() == null)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private Friendship findPendingRequestForUser(UUID friendshipId, UUID addresseeId) {
    Friendship friendship =
        friendshipRepository
            .findById(friendshipId)
            .orElseThrow(() -> new FriendshipNotFoundException(friendshipId));

    if (!friendship.getAddressee().getId().equals(addresseeId)) {
      throw new FriendshipNotFoundException(
          "Friendship " + friendshipId + " does not belong to user " + addresseeId);
    }
    if (friendship.getStatus() != FriendshipStatus.PENDING) {
      throw new ConflictException("Friendship is not in PENDING state: " + friendship.getStatus());
    }
    return friendship;
  }

  private void invalidateLeaderboardCache(UUID userId) {
    try {
      redisTemplate.delete(LEADERBOARD_CACHE_PREFIX + userId);
    } catch (Exception ex) {
      log.debug("Failed to invalidate leaderboard cache for userId={}: {}", userId, ex.getMessage());
    }
  }
}
