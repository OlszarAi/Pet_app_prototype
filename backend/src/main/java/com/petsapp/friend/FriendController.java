package com.petsapp.friend;

import com.petsapp.auth.User;
import com.petsapp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy modulu znajomych.
 *
 * <p>Wszystkie endpointy wymagaja uwierzytelnienia JWT.
 */
@RestController
@RequestMapping("/friends")
@Tag(name = "Friends", description = "Zarzadzanie znajomosciami")
public class FriendController {

  private final FriendService friendService;

  public FriendController(FriendService friendService) {
    this.friendService = friendService;
  }

  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Wyslij prosbe o znajomosc")
  public ApiResponse<FriendResponse> sendRequest(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody SendFriendRequestRequest request) {
    return ApiResponse.ok(friendService.sendRequest(currentUser, request.targetUserId()));
  }

  @GetMapping("/requests/pending")
  @Operation(summary = "Pobierz oczekujace prosby o znajomosc")
  public ApiResponse<List<FriendResponse>> getPendingRequests(
      @AuthenticationPrincipal User currentUser) {
    return friendService.getPendingRequests(currentUser);
  }

  @PatchMapping("/requests/{friendshipId}/accept")
  @Operation(summary = "Akceptuj prosbe o znajomosc")
  public ApiResponse<FriendResponse> acceptRequest(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID friendshipId) {
    return ApiResponse.ok(friendService.acceptRequest(currentUser, friendshipId));
  }

  @DeleteMapping("/requests/{friendshipId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Odrzuc prosbe o znajomosc")
  public void rejectRequest(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID friendshipId) {
    friendService.rejectRequest(currentUser, friendshipId);
  }

  @GetMapping
  @Operation(summary = "Pobierz liste znajomych")
  public ApiResponse<List<FriendResponse>> getFriends(@AuthenticationPrincipal User currentUser) {
    return friendService.getFriends(currentUser);
  }

  @DeleteMapping("/{targetUserId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Usun znajomego (unfriend)")
  public void removeFriend(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID targetUserId) {
    friendService.removeFriend(currentUser, targetUserId);
  }

  @PostMapping("/block/{targetUserId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Zablokuj uzytkownika")
  public void blockUser(
      @AuthenticationPrincipal User currentUser, @PathVariable UUID targetUserId) {
    friendService.blockUser(currentUser, targetUserId);
  }

  @GetMapping("/leaderboard")
  @Operation(summary = "Leaderboard znajomych (posortowane po unique_breeds)")
  public ApiResponse<List<LeaderboardEntryResponse>> getLeaderboard(
      @AuthenticationPrincipal User currentUser) {
    return friendService.getLeaderboard(currentUser);
  }
}
