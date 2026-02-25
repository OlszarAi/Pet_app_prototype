package com.petsapp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /**
     * Szuka aktywnego (nieuzytego) kodu weryfikacyjnego dla danego uzytkownika.
     *
     * <p>
     * Jeden uzytkownik moze miec wiele rekordow (po resend), ale aktywny jest tylko
     * ten z used_at IS NULL i nieprzeterminowany.
     */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.user.id = :userId "
            + "AND ev.usedAt IS NULL ORDER BY ev.createdAt DESC")
    Optional<EmailVerification> findLatestUnusedByUserId(@Param("userId") UUID userId);

    @Query("SELECT ev FROM EmailVerification ev WHERE ev.user.id = :userId "
            + "AND ev.code = :code AND ev.usedAt IS NULL")
    Optional<EmailVerification> findByUserIdAndCode(
            @Param("userId") UUID userId, @Param("code") String code);
}
