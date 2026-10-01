package com.tcm.shortlink;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Follows a short link (TCM-35): {@code GET /s/<code>} redirects to the link
 * it stands for. It is opened straight from a phone's camera app, with no
 * token to send, so it is public - and it gives nothing away the QR code on
 * the screen didn't already. Whatever it leads to still does its own checks
 * (the check-in page asks the student to sign in).
 *
 * The frontend proxies {@code /s/} here (nginx.conf, vite.config.js), so the
 * link stays on the frontend's origin and as short as it can be.
 *
 * A code that doesn't exist or has expired lands on the check-in page,
 * where the student can scan the code that is on screen now.
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @GetMapping("/s/{code}")
    public ResponseEntity<Void> follow(@PathVariable String code) {
        String target = shortLinkService.resolve(code).orElseGet(shortLinkService::fallbackUrl);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
