package com.tcm.shortlink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A short code standing in for a long link (TCM-35). The code is the key:
 * {@code <frontend>/s/<code>} is what gets shown and encoded, and resolving
 * it hands back {@link #targetUrl}. Every link expires - the only links
 * shortened today are QR check-ins, which are short-lived themselves.
 */
@Entity
@Table(name = "short_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortLink {

    @Id
    @Column(name = "code", length = 16, updatable = false, nullable = false)
    private String code;

    @Column(name = "target_url", nullable = false, length = 2048, updatable = false)
    private String targetUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
}
