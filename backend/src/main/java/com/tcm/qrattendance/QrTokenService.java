package com.tcm.qrattendance;

import com.tcm.common.BadRequestException;
import com.tcm.common.GoneException;
import com.tcm.common.ResourceNotFoundException;
import com.tcm.schedule.ClassSessionRepository;
import com.tcm.schedule.model.ClassSession;
import com.tcm.schedule.model.SessionStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mints and checks the short-lived token behind a session's QR code
 * (TCM-27), stored in the {@code qr_token}/{@code qr_expires_at} columns
 * TCM-17 put on {@code class_sessions}.
 *
 * The token is {@code <random>.<hmac>}: the random half is what's stored and
 * compared, the signature half lets a forged token be rejected on sight,
 * before any database lookup. Both halves are checked - a signature alone
 * would still let a token that has been replaced by a newer one through.
 *
 * One token per session at a time: regenerating overwrites the column, so
 * the previous code on the previous screen stops working immediately.
 */
@Service
@RequiredArgsConstructor
public class QrTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final ClassSessionRepository classSessionRepository;

    @Value("${app.qr.secret}")
    private String secret;

    @Value("${app.qr.validity-minutes}")
    private long validityMinutes;

    /**
     * Replaces the session's token with a fresh one.
     *
     * @param requesterIsAdmin whether the caller holds ROLE_ADMIN - anyone
     *                         else may only do this for a session they are
     *                         the assigned trainer of.
     */
    @Transactional
    public IssuedToken issue(UUID sessionId, UUID requesterId, boolean requesterIsAdmin) {
        ClassSession session = getOrThrow(sessionId);
        if (!requesterIsAdmin && !session.getTrainer().getId().equals(requesterId)) {
            throw new AccessDeniedException("You may only produce a QR code for a session you are assigned to");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This session was cancelled, so there is nothing to check in to");
        }

        String random = ENCODER.encodeToString(randomBytes());
        String token = random + "." + sign(sessionId, random);
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(validityMinutes));

        session.setQrToken(token);
        session.setQrExpiresAt(expiresAt);
        classSessionRepository.save(session);
        return new IssuedToken(token, expiresAt);
    }

    /**
     * The session a valid token belongs to.
     *
     * @throws GoneException       if the token was this session's but has expired
     * @throws BadRequestException if it was never this session's, or is malformed
     */
    @Transactional(readOnly = true)
    public ClassSession requireValid(UUID sessionId, String token) {
        ClassSession session = getOrThrow(sessionId);
        String[] parts = token == null ? new String[0] : token.split("\\.", 2);

        if (parts.length != 2 || !constantTimeEquals(sign(sessionId, parts[0]), parts[1])) {
            throw new BadRequestException("That QR code is not valid for this session");
        }
        if (session.getQrToken() == null || !constantTimeEquals(session.getQrToken(), token)) {
            // A correctly signed token that isn't the current one: a code
            // from an earlier screen, replaced by a newer one.
            throw new GoneException("That QR code has been replaced by a newer one - ask for the current code");
        }
        if (session.getQrExpiresAt() == null || session.getQrExpiresAt().isBefore(Instant.now())) {
            throw new GoneException("That QR code has expired - ask the trainer to show a fresh one");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BadRequestException("This session was cancelled, so there is nothing to check in to");
        }
        return session;
    }

    private String sign(UUID sessionId, String random) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return ENCODER.encodeToString(
                    mac.doFinal((sessionId + "." + random).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign the QR token", e);
        }
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /** Comparison that doesn't leak how much of a guess was right. */
    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private ClassSession getOrThrow(UUID sessionId) {
        return classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No session with id " + sessionId));
    }

    /** A freshly minted token and when it stops working. */
    public record IssuedToken(String token, Instant expiresAt) {
    }
}
