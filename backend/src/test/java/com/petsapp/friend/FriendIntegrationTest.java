package com.petsapp.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.common.ConflictException;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.common.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla FriendService z prawdziwa baza PostgreSQL.
 *
 * <p>Weryfikuje pelny cykl zycia relacji znajomosci: wyslanie prosby,
 * akceptacja, odrzucenie, unfriend, blokowanie i leaderboard.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FriendIntegrationTest extends AbstractIntegrationTest {

  @Autowired private FriendService friendService;
  @Autowired private FriendshipRepository friendshipRepository;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;

  @MockBean private EmailService emailService;

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  private User createVerifiedUser() {
    String email = "friend-test-" + UUID.randomUUID() + "@example.com";
    String username = "ft" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    String code = "111222";
    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, "Password123!"));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow();
  }

  @Nested
  class SendRequest {

    @Test
    void sendRequest_newRelationship_createsPending() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse response = friendService.sendRequest(alice, bob.getId());

      assertThat(response.status()).isEqualTo("pending");
      assertThat(response.userId()).isEqualTo(bob.getId());
    }

    @Test
    void sendRequest_toYourself_throwsConflict() {
      User alice = createVerifiedUser();

      assertThatThrownBy(() -> friendService.sendRequest(alice, alice.getId()))
          .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendRequest_duplicateRequest_throwsConflict() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      friendService.sendRequest(alice, bob.getId());

      assertThatThrownBy(() -> friendService.sendRequest(alice, bob.getId()))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("Friendship already exists");
    }
  }

  @Nested
  class AcceptRequest {

    @Test
    void acceptRequest_validPendingRequest_setsAccepted() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());

      FriendResponse accepted = friendService.acceptRequest(bob, pending.friendshipId());

      assertThat(accepted.status()).isEqualTo("accepted");
    }

    @Test
    void acceptRequest_wrongUser_throwsNotFound() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();
      User carol = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());

      // Carol nie jest addressee tej prosby
      assertThatThrownBy(() -> friendService.acceptRequest(carol, pending.friendshipId()))
          .isInstanceOf(FriendshipNotFoundException.class);
    }
  }

  @Nested
  class RejectRequest {

    @Test
    void rejectRequest_validPendingRequest_deletesFriendship() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());
      friendService.rejectRequest(bob, pending.friendshipId());

      assertThat(friendshipRepository.findBetween(alice.getId(), bob.getId())).isEmpty();
    }
  }

  @Nested
  class RemoveFriend {

    @Test
    void removeFriend_acceptedFriendship_deletesRelationship() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());
      friendService.acceptRequest(bob, pending.friendshipId());

      friendService.removeFriend(alice, bob.getId());

      assertThat(friendshipRepository.findBetween(alice.getId(), bob.getId())).isEmpty();
    }

    @Test
    void removeFriend_pendingRequest_throwsNotFound() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      friendService.sendRequest(alice, bob.getId());

      assertThatThrownBy(() -> friendService.removeFriend(alice, bob.getId()))
          .isInstanceOf(FriendshipNotFoundException.class);
    }
  }

  @Nested
  class BlockUser {

    @Test
    void blockUser_noExistingRelationship_createsBlockedEntry() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      friendService.blockUser(alice, bob.getId());

      assertThat(friendshipRepository.findBetween(alice.getId(), bob.getId()))
          .isPresent()
          .get()
          .satisfies(f -> assertThat(f.getStatus()).isEqualTo(FriendshipStatus.BLOCKED));
    }

    @Test
    void blockUser_existingAcceptedFriendship_replacesWithBlocked() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());
      friendService.acceptRequest(bob, pending.friendshipId());

      friendService.blockUser(alice, bob.getId());

      assertThat(friendshipRepository.findBetween(alice.getId(), bob.getId()))
          .isPresent()
          .get()
          .satisfies(f -> assertThat(f.getStatus()).isEqualTo(FriendshipStatus.BLOCKED));
    }
  }

  @Nested
  class GetFriends {

    @Test
    void getFriends_afterAccept_returnsFriend() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());
      friendService.acceptRequest(bob, pending.friendshipId());

      ApiResponse<List<FriendResponse>> response = friendService.getFriends(alice);

      assertThat(response.data()).hasSize(1);
      assertThat(response.data().get(0).userId()).isEqualTo(bob.getId());
    }

    @Test
    void getPendingRequests_afterSend_returnsRequest() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      friendService.sendRequest(alice, bob.getId());

      ApiResponse<List<FriendResponse>> response = friendService.getPendingRequests(bob);

      assertThat(response.data()).hasSize(1);
      assertThat(response.data().get(0).userId()).isEqualTo(alice.getId());
    }
  }

  @Nested
  class Leaderboard {

    @Test
    void getLeaderboard_includeSelfInResults() {
      User alice = createVerifiedUser();
      User bob = createVerifiedUser();

      FriendResponse pending = friendService.sendRequest(alice, bob.getId());
      friendService.acceptRequest(bob, pending.friendshipId());

      ApiResponse<List<LeaderboardEntryResponse>> response = friendService.getLeaderboard(alice);

      // Oboje alice i bob sa w rankingu
      assertThat(response.data()).hasSize(2);
      // Ranki sa przypisane od 1
      assertThat(response.data().stream().map(LeaderboardEntryResponse::rank))
          .containsExactlyInAnyOrder(1, 2);
    }
  }
}
