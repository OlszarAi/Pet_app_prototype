package com.petsapp.friend;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO wejscia do wysylania prosb o znajomosc.
 */
public record SendFriendRequestRequest(@NotNull UUID targetUserId) {}
