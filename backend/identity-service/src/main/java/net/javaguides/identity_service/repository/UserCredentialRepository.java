package net.javaguides.identity_service.repository;

import net.javaguides.identity_service.dto.LoginChartProjection;
import net.javaguides.identity_service.entity.UserCredential;
import net.javaguides.identity_service.enums.ERole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findFirstByEmailAndDelFlgFalse(String email);
    Optional<UserCredential> findByIdAndDelFlgFalse(Long id);

    // deleted users get a "deleted_<id>_" prefix on name/email (unique constraints), so these check all rows
    boolean existsByName(String name);
    boolean existsByEmail(String email);
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    Page<UserCredential> findByDelFlgFalse(Pageable pageable);

    // count total user active
    long countByDelFlgTrue();

    @Query("""
           SELECT COUNT(DISTINCT u.id)
           FROM UserCredential u
           JOIN u.roles r
           WHERE r.name = :role AND u.delFlg = false
           """)
    long countActiveByRole(@Param("role") ERole role);

    // LEFT JOIN: CUSTOMER has no permissions, an inner join would drop the user
    @Query("""
           SELECT DISTINCT u
           FROM UserCredential u
           LEFT JOIN FETCH u.roles r
           LEFT JOIN FETCH r.permissions
           WHERE u.name = :username AND u.delFlg = false
           """)
    Optional<UserCredential> findActiveByNameWithPermissions(@Param("username") String username);

    @Query("""
           SELECT DISTINCT u
           FROM UserCredential u
           LEFT JOIN FETCH u.roles r
           LEFT JOIN FETCH r.permissions
           WHERE u.id = :userId AND u.delFlg = false
           """)
    Optional<UserCredential> findActiveByIdWithPermissions(@Param("userId") Long userId);

    @Query("""
           SELECT DISTINCT u
           FROM UserCredential u
           LEFT JOIN FETCH u.roles r
           LEFT JOIN FETCH r.permissions
           WHERE u.email = :email AND u.delFlg = false
           """)
    Optional<UserCredential> findActiveByEmailWithPermissions(@Param("email") String email);

    @Query("""
      SELECT COUNT(u.id)
      FROM UserCredential u
      WHERE u.lastLoginAt >= :threshold
    """)
    long countOnlineUsers(@Param("threshold") LocalDateTime threshold);

    @Query("""
      SELECT DATE(u.lastLoginAt) as date, COUNT(u.id) as count
      FROM UserCredential u
      WHERE u.lastLoginAt >= :from
      GROUP BY DATE(u.lastLoginAt)
      ORDER BY DATE(u.lastLoginAt)
    """)
    List<LoginChartProjection> countLoginByDate(
            @Param("from") LocalDateTime from
    );

    @Query("""
      SELECT COUNT(u.id)
      FROM UserCredential u
      WHERE u.lastLoginAt >= :start
        AND u.lastLoginAt < :end
    """)
    long countLoginToday(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
