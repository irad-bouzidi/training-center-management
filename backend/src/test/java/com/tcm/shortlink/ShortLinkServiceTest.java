package com.tcm.shortlink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcm.shortlink.model.ShortLink;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    private static final String FRONTEND = "http://localhost:5173";
    private static final String TARGET = FRONTEND + "/attend/some-session?token=abc.def";

    @Mock
    private ShortLinkRepository shortLinkRepository;

    private ShortLinkService shortLinkService;

    @BeforeEach
    void setUp() {
        shortLinkService = new ShortLinkService(shortLinkRepository);
        ReflectionTestUtils.setField(shortLinkService, "frontendBaseUrl", FRONTEND);
    }

    @Test
    void shorten_storesARandomCodeForTheTarget_andReturnsTheShortUrl() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(shortLinkRepository.existsById(anyString())).thenReturn(false);

        String shortUrl = shortLinkService.shorten(TARGET, expiresAt);

        ArgumentCaptor<ShortLink> saved = ArgumentCaptor.forClass(ShortLink.class);
        verify(shortLinkRepository).save(saved.capture());
        String code = saved.getValue().getCode();
        assertThat(code).hasSize(ShortLinkService.CODE_LENGTH)
                .matches("[" + ShortLinkService.ALPHABET + "]+");
        assertThat(saved.getValue().getTargetUrl()).isEqualTo(TARGET);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(expiresAt);
        assertThat(shortUrl).isEqualTo(FRONTEND + "/s/" + code);
    }

    @Test
    void shorten_clearsOutExpiredLinks() {
        when(shortLinkRepository.existsById(anyString())).thenReturn(false);

        shortLinkService.shorten(TARGET, Instant.now().plusSeconds(600));

        verify(shortLinkRepository).deleteExpiredBefore(any(Instant.class));
    }

    @Test
    void shorten_drawsAgainOnACollision() {
        when(shortLinkRepository.existsById(anyString())).thenReturn(true, false);

        shortLinkService.shorten(TARGET, Instant.now().plusSeconds(600));

        verify(shortLinkRepository, org.mockito.Mockito.times(2)).existsById(anyString());
    }

    @Test
    void resolve_aLiveLink_givesItsTarget() {
        when(shortLinkRepository.findById("abc2345")).thenReturn(Optional.of(
                new ShortLink("abc2345", TARGET, Instant.now(), Instant.now().plusSeconds(600))));

        assertThat(shortLinkService.resolve("abc2345")).contains(TARGET);
    }

    @Test
    void resolve_anExpiredLink_givesNothing() {
        when(shortLinkRepository.findById("abc2345")).thenReturn(Optional.of(
                new ShortLink("abc2345", TARGET, Instant.now().minusSeconds(900), Instant.now().minusSeconds(1))));

        assertThat(shortLinkService.resolve("abc2345")).isEmpty();
    }

    @Test
    void resolve_anUnknownCode_givesNothing() {
        when(shortLinkRepository.findById("nope234")).thenReturn(Optional.empty());

        assertThat(shortLinkService.resolve("nope234")).isEmpty();
    }
}
