package io.gsi.hive.platform.player.play;

import static org.hibernate.annotations.QueryHints.FETCH_SIZE;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface PlayRepository extends JpaRepository<Play,String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Play> findAndLockByPlayId(String playId);

    @Query(value =
        "SELECT * FROM t_play p WHERE status = 'ACTIVE' " +
        "AND NOT EXISTS (SELECT 1 FROM t_txn as t WHERE t.play_id = p.play_id AND t.status IN ('RECON','PENDING')) "+
        "AND NOT EXISTS (SELECT 1 FROM t_autocomplete_request_q as q WHERE q.play_id = p.play_id);"
        , nativeQuery = true)
    @QueryHints(value = @QueryHint(name = FETCH_SIZE, value = "250"))
    Stream<Play> findAllActive();

    @Query(value =
     "SELECT * FROM t_play p WHERE status = :playStatus " +
     "AND created_at < :cutoffTs AND igp_code = :igpCode " +
     "AND NOT EXISTS (SELECT 1 FROM t_txn as t WHERE t.play_id = p.play_id AND t.status IN ('RECON','PENDING')) "+
     "AND NOT EXISTS (SELECT 1 FROM t_autocomplete_request_q as q WHERE q.play_id = p.play_id);"
        , nativeQuery = true)
    List<Play> findAllByStatusAndIgpCodeAndCreatedAtBefore(
        @Param("playStatus") String playStatus,
        @Param("igpCode") String igpCode,
        @Param("cutoffTs") Calendar cutoffTs
    );

    @Query(value =
        "SELECT * FROM t_play p WHERE status = :playStatus " +
            "AND igp_code = :igpCode " +
            "AND NOT EXISTS (SELECT 1 FROM t_txn as t WHERE t.play_id = p.play_id AND t.status IN ('RECON','PENDING')) "+
            "AND NOT EXISTS (SELECT 1 FROM t_autocomplete_request_q as q WHERE q.play_id = p.play_id);"
        , nativeQuery = true)
    List<Play> findAllByStatusAndIgpCodeWhereNotQueuedOrStatusRecon(
        @Param("playStatus") String playStatus,
        @Param("igpCode") String igpCode
    );
}
