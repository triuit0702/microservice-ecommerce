package net.javaguides.identity_service.repository;

import net.javaguides.identity_service.dto.LoginChartProjection;
import net.javaguides.identity_service.entity.UserCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByNameAndDelFlgFalse(String username);
    Page<UserCredential> findByDelFlgFalse(Pageable pageable);

    // count total user active
    long countByDelFlgTrue();

    // Lấy user kèm roles + permissions
    @Query("""
           SELECT DISTINCT u
           FROM UserCredential u
           JOIN FETCH u.roles r
           JOIN FETCH r.permissions
           WHERE u.name = :username and u.delFlg = false
           """)
    Optional<UserCredential> findByUsernameWithPermissions(@Param("username") String username);

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
