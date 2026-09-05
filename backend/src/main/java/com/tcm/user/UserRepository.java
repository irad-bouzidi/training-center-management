package com.tcm.user;

import com.tcm.user.model.Role;
import com.tcm.user.model.User;
import com.tcm.user.model.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // CAST(:name AS string) is required: when :name is null, Hibernate can't
    // infer its SQL type from the Java value alone, and Postgres ends up
    // trying LOWER(bytea) instead of LOWER(text) - which fails outright.
    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:name IS NULL
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
            """)
    Page<User> search(@Param("role") Role role,
                       @Param("status") UserStatus status,
                       @Param("name") String name,
                       Pageable pageable);
}
