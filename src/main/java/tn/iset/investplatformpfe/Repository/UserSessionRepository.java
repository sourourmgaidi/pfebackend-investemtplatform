package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.UserSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByUserEmailAndLogoutTimeIsNull(String userEmail);

    List<UserSession> findByUserEmailAndLoginTimeBetween(String userEmail, LocalDateTime start, LocalDateTime end);

    List<UserSession> findAllByLogoutTimeIsNull();

    // ✅ Sessions totales pour une période
    @Query("SELECT COALESCE(SUM(s.durationSeconds), 0) FROM UserSession s " +
            "WHERE s.userEmail = :userEmail " +
            "AND s.loginTime BETWEEN :start AND :end " +
            "AND s.durationSeconds IS NOT NULL")
    Long calculateTotalSecondsForPeriod(@Param("userEmail") String userEmail,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    // ✅ Stats journalières — FUNCTION('DATE') est du JPQL standard
    @Query("SELECT FUNCTION('DATE', s.loginTime), COALESCE(SUM(s.durationSeconds), 0) " +
            "FROM UserSession s " +
            "WHERE s.userEmail = :userEmail " +
            "AND s.loginTime BETWEEN :start AND :end " +
            "AND s.durationSeconds IS NOT NULL " +
            "GROUP BY FUNCTION('DATE', s.loginTime) " +
            "ORDER BY FUNCTION('DATE', s.loginTime)")
    List<Object[]> getDailyStats(@Param("userEmail") String userEmail,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    // ✅ Sessions détaillées pour une période
    @Query("SELECT s FROM UserSession s " +
            "WHERE s.userEmail = :userEmail " +
            "AND s.loginTime BETWEEN :start AND :end " +
            "AND s.durationSeconds IS NOT NULL " +
            "ORDER BY s.loginTime DESC")
    List<UserSession> getSessionsByPeriod(@Param("userEmail") String userEmail,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // ✅ CORRIGÉ: YEAR() et WEEK() → FUNCTION('YEAR',...) et FUNCTION('WEEK',...)
    @Query("SELECT FUNCTION('YEAR', s.loginTime), FUNCTION('WEEK', s.loginTime), " +
            "COALESCE(SUM(s.durationSeconds), 0) " +
            "FROM UserSession s " +
            "WHERE s.userEmail = :userEmail " +
            "AND s.loginTime BETWEEN :start AND :end " +
            "AND s.durationSeconds IS NOT NULL " +
            "GROUP BY FUNCTION('YEAR', s.loginTime), FUNCTION('WEEK', s.loginTime) " +
            "ORDER BY FUNCTION('YEAR', s.loginTime) DESC, FUNCTION('WEEK', s.loginTime) DESC")
    List<Object[]> getWeeklyStats(@Param("userEmail") String userEmail,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    // ✅ AJOUT: Query ciblée pour le nettoyage — évite le findAll() en mémoire
    @Query("SELECT s FROM UserSession s " +
            "WHERE s.logoutTime IS NULL " +
            "AND s.loginTime < :cutoff")
    List<UserSession> findOrphanedSessions(@Param("cutoff") LocalDateTime cutoff);

    // ✅ AJOUT: Vérifier si une session active existe (utile pour debug)
    @Query("SELECT COUNT(s) > 0 FROM UserSession s " +
            "WHERE s.userEmail = :email " +
            "AND s.logoutTime IS NULL")
    boolean hasActiveSession(@Param("email") String email);
}