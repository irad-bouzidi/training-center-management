package com.tcm.certificate;

import com.tcm.certificate.dto.CertificateRequest;
import com.tcm.certificate.dto.CertificateResponse;
import com.tcm.security.UserPrincipal;
import com.tcm.user.model.Role;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Certificate issue and download, per docs/tasks/TCM-25. The role
 * annotations only say who may ask at all; which course a trainer may
 * certify on, and whose certificate anyone may read, are ownership questions
 * the service settles.
 */
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TRAINER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CertificateResponse generate(@Valid @RequestBody CertificateRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return certificateService.generate(
                request.studentId(), request.courseId(), principal.getId(), isAdmin(principal));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        CertificateService.DownloadableCertificate certificate =
                certificateService.download(id, principal.getId(), isAdmin(principal));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(certificate.filename()).build().toString())
                .body(certificate.content());
    }

    /** A student may omit {@code studentId} and get their own. */
    @GetMapping
    public List<CertificateResponse> forStudent(@RequestParam(required = false) UUID studentId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return certificateService.findForStudent(
                studentId == null ? principal.getId() : studentId, principal.getId(), isAdmin(principal));
    }

    private static boolean isAdmin(UserPrincipal principal) {
        return principal.getUser().getRole() == Role.ADMIN;
    }
}
