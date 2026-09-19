package com.tcm.certificate;

import com.tcm.certificate.model.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    List<Certificate> findByStudentIdOrderByIssuedAtDesc(UUID studentId);

    List<Certificate> findByCourseIdOrderByIssuedAtDesc(UUID courseId);

    Optional<Certificate> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    /**
     * The next certificate number's sequence value. Counting rows in Java
     * would let two admins generating at the same moment mint the same
     * number; the database's own sequence can't.
     */
    @Query(value = "select nextval('certificate_number_seq')", nativeQuery = true)
    long nextCertificateSequence();
}
