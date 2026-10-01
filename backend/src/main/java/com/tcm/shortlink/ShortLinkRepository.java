package com.tcm.shortlink;

import com.tcm.shortlink.model.ShortLink;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ShortLinkRepository extends JpaRepository<ShortLink, String> {

    /** Housekeeping: a link past its expiry can never resolve again. */
    @Modifying
    @Query("delete from ShortLink l where l.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
