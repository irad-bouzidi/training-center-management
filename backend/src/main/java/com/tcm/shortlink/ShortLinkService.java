package com.tcm.shortlink;

import com.tcm.shortlink.model.ShortLink;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The link shortener (TCM-35): swaps a long link for {@code <frontend>/s/<code>},
 * which {@link ShortLinkController} answers with a redirect to the long one.
 * The frontend only ever shows and opens these links - all of the
 * shortening and resolving is here.
 *
 * Codes are random rather than sequential, so one can't be guessed from
 * another, and drawn from an alphabet without look-alikes (0/O, 1/l/I) so a
 * link read off a projector can be typed back in. Seven characters of 56 is
 * about 1.7e12 codes - plenty for links that live minutes.
 */
@Service
@RequiredArgsConstructor
public class ShortLinkService {

    static final String ALPHABET = "23456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShortLinkRepository shortLinkRepository;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    /**
     * A short link to {@code targetUrl}, valid until {@code expiresAt}.
     *
     * @return the full short URL, {@code <frontend>/s/<code>}
     */
    @Transactional
    public String shorten(String targetUrl, Instant expiresAt) {
        Instant now = Instant.now();
        shortLinkRepository.deleteExpiredBefore(now);

        ShortLink link = ShortLink.builder()
                .code(freshCode())
                .targetUrl(targetUrl)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
        shortLinkRepository.save(link);
        return urlFor(link.getCode());
    }

    /** Where a short code leads - empty if there's no such code, or it has expired. */
    @Transactional(readOnly = true)
    public Optional<String> resolve(String code) {
        return shortLinkRepository.findById(code)
                .filter(link -> link.getExpiresAt().isAfter(Instant.now()))
                .map(ShortLink::getTargetUrl);
    }

    /** Where a dead short link sends people instead: the check-in page, to scan the current code. */
    public String fallbackUrl() {
        return frontendBaseUrl + "/attend";
    }

    public String urlFor(String code) {
        return "%s/s/%s".formatted(frontendBaseUrl, code);
    }

    private String freshCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!shortLinkRepository.existsById(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not find a free short link code");
    }

    private static String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
