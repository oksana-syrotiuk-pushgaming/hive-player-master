package io.gsi.hive.platform.player.persistence;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;

public interface AutocompleteRequestRepository extends JpaRepository<AutocompleteRequest, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    AutocompleteRequest findAndLockByPlayId(String playId);

    @Query(value = "SELECT * FROM t_autocomplete_request_q r WHERE r.retries <= :retryLimit LIMIT :size", nativeQuery = true)
    List<AutocompleteRequest> getQueuedRequests (
        @Param("size") Integer size, @Param("retryLimit") Integer retryLimit);
}


