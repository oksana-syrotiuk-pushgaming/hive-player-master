package io.gsi.hive.platform.player.session;

import java.util.List;

import org.postgresql.util.PGInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, String>{

	void deleteByLastAccessedTimeLessThan(long expires);

    List<Session> findByStatusAndPlayerIdAndIgpCode(SessionStatus sessionStatus, String playerId, String igpCode);

    List<Session> findByStatusAndPlayerIdAndIgpCodeAndAuthenticated(SessionStatus sessionStatus, String playerId, String igpCode, boolean authenticated);

    GameplaySession findBySessionToken(String sessionToken);

    @Query(nativeQuery = true, value=setInactiveSessionsToExpiredBatched)
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    void batchExpireInactiveSessions(@Param("size") Integer size, @Param("expiryTime") Integer expiryTime);

    String setInactiveSessionsToExpiredBatched = "WITH expired_sessions as (" +
            "SELECT * FROM t_session " +
            "WHERE status = 'ACTIVE' " +
            "AND (last_accessed_time / 1000) + :expiryTime < extract(epoch from now()) " +
            "LIMIT :size " +
            ") " +
            "UPDATE t_session " +
            "SET status = 'EXPIRED' " +
            "FROM expired_sessions " +
            "WHERE t_session.session_id = expired_sessions.session_id ";

    @Query(nativeQuery = true, value = setAllInactiveSessionsToExpired)
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    void expireAllInactiveSessions(@Param("expiryTime") Integer expiryTime);

    String setAllInactiveSessionsToExpired = "UPDATE t_session " +
            "SET status = 'EXPIRED' " +
            "WHERE status = 'ACTIVE' " +
            "AND (last_accessed_time / 1000) + :expiryTime < extract(epoch from now())";
}
