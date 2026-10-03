package com.tcm.certificate;

import com.tcm.common.BadRequestException;
import java.util.Locale;

/**
 * The languages every certificate is drawn in. Both are generated when the
 * certificate is issued, so a student can hand whichever one is asked for
 * without anyone having to reissue it.
 */
public enum CertificateLanguage {

    EN(Locale.ENGLISH),
    FR(Locale.FRENCH);

    private final Locale locale;

    CertificateLanguage(Locale locale) {
        this.locale = locale;
    }

    public Locale locale() {
        return locale;
    }

    /** The lowercase code used in URLs and filenames - {@code en}, {@code fr}. */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** {@code en}/{@code fr}, any case; {@code null} means English. */
    public static CertificateLanguage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return EN;
        }
        for (CertificateLanguage language : values()) {
            if (language.code().equalsIgnoreCase(code.trim())) {
                return language;
            }
        }
        throw new BadRequestException("Unsupported certificate language: " + code);
    }
}
