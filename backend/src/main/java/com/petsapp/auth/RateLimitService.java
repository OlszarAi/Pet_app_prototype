package com.petsapp.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.EstimationProbe;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Rate limiting dla endpointow auth oparty na Bucket4j.
 *
 * <p>
 * W tej implementacji (Krok 2) uzywamy lokalnej mapy in-memory per instancja
 * JVM. Pozwala to na
 * pelne testy bez Redis. Dla produkcji / wielu instancji nalezy zastapic
 * ConcurrentHashMap przez
 * Bucket4j-Redis (ProxyManager z config.redis.asAsync...) — to zostanie dodane
 * w etapie CI/CD.
 *
 * <p>
 * Klucze: "{endpoint}:{ipAddress}" — granularnosc per endpoint i per klient.
 */
@Service
public class RateLimitService {

    private static final int LOGIN_TOKENS_PER_MINUTE = 5;
    private static final int REGISTER_TOKENS_PER_MINUTE = 3;
    private static final int RESEND_TOKENS_PER_MINUTE = 2;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Sprawdza czy IP moze wykonac kolejne zadanie logowania.
     *
     * @param ipAddress adres IP klienta
     * @throws RateLimitExceededException gdy limit zostal przekroczony
     */
    public void checkLoginRateLimit(String ipAddress) {
        checkLimit("login", ipAddress, LOGIN_TOKENS_PER_MINUTE);
    }

    /**
     * Sprawdza czy IP moze wykonac kolejne zadanie rejestracji.
     *
     * @param ipAddress adres IP klienta
     * @throws RateLimitExceededException gdy limit zostal przekroczony
     */
    public void checkRegisterRateLimit(String ipAddress) {
        checkLimit("register", ipAddress, REGISTER_TOKENS_PER_MINUTE);
    }

    /**
     * Sprawdza czy IP moze ponownie wyslac email weryfikacyjny.
     *
     * @param ipAddress adres IP klienta
     * @throws RateLimitExceededException gdy limit zostal przekroczony
     */
    public void checkResendRateLimit(String ipAddress) {
        checkLimit("resend", ipAddress, RESEND_TOKENS_PER_MINUTE);
    }

    private void checkLimit(String endpoint, String ipAddress, int tokensPerMinute) {
        String key = endpoint + ":" + ipAddress;
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(tokensPerMinute));

        EstimationProbe probe = bucket.estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Try again in " + waitSeconds + " seconds.");
        }
        bucket.tryConsume(1);
    }

    private Bucket buildBucket(int tokensPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(tokensPerMinute)
                .refillGreedy(tokensPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
