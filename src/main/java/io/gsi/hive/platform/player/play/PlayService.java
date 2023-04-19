package io.gsi.hive.platform.player.play;

import io.gsi.commons.exception.InvalidStateException;
import io.gsi.hive.platform.player.cache.CacheConfig;
import io.gsi.hive.platform.player.exception.PlayNotFoundException;
import io.gsi.hive.platform.player.play.search.PlaySearchArguments;
import io.gsi.hive.platform.player.play.search.PlaySearchService;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.event.BonusFundDetails;
import io.gsi.hive.platform.player.txn.event.TxnCleardownEvent;
import io.gsi.hive.platform.player.txn.event.TxnReceipt;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PlayService {

    private final PlayRepository playRepository;
    private final PlaySearchService playSearchService;

    public PlayService(PlayRepository playRepository, PlaySearchService playSearchService) {
        this.playRepository = playRepository;
        this.playSearchService = playSearchService;
    }

    @Transactional
    public void addTxn(Txn txn) {
        if(txn.isStake()) {
            Play play = playRepository.findById(txn.getPlayId()).orElse(null);

            Optional<BonusFundDetails> bonusDetails = txn.getEvents().stream()
                    .filter(TxnRequest.class::isInstance)
                    .map(TxnRequest.class::cast)
                    .findFirst()
                    .map(TxnRequest::getBonusFundDetails);

            if(play == null) {
                play = new Play();
                play.setCcyCode(txn.getCcyCode());
                play.setIgpCode(txn.getIgpCode());
                play.setGameCode(txn.getGameCode());
                play.setGuest(txn.getGuest());
                play.setAutoCompleted(false);
                play.setMode(txn.getMode());
                play.setStake(txn.getAmount());
                play.setWin(new BigDecimal(0));
                play.setCreatedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                play.setPlayerId(txn.getPlayerId());
                play.setPlayId(txn.getPlayId());
                play.setNumTxns(1);
                play.setPlayRef(txn.getPlayRef());
                if(bonusDetails.isPresent()) {
                    play.setBonusFundType(bonusDetails.get().getType());
                }
                play.setStatus(PlayStatus.ACTIVE);
                play.setSessionId(txn.getSessionId());
            } else {
                play.setStake(play.getStake().add(txn.getAmount()));
                play.setNumTxns(play.getNumTxns()+1);
            }
            play.setModifiedAt(ZonedDateTime.now(ZoneId.of("UTC")));

            playRepository.save(play);
        }
    }

    @Transactional
    public void cancelStake(Txn txn) {
        Play plays = playRepository.findById(txn.getPlayId()).get();
        if(plays != null) {
            if (txn.isPlayCompleteIfCancelled()) {
                plays.setModifiedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                plays.setStake(plays.getStake().subtract(txn.getAmount()));
                plays.setStatus(PlayStatus.VOIDED);
            } else {
                plays.setModifiedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                plays.setStake(plays.getStake().subtract(txn.getAmount()));
                plays.setStatus(PlayStatus.ACTIVE);
            }
            playRepository.save(plays);
         }
    }

    @Transactional
    public Play voidPlay(String playId){
        Play play = playRepository.findById(playId)
            .orElseThrow(PlayNotFoundException::new);
        switch(play.getStatus()){
            //ACTIVE allow voiding
            case ACTIVE:
                play.setModifiedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                play.setStatus(PlayStatus.VOIDED);
                playRepository.save(play);
                break;
            case VOIDED:
            	break;
            default:
            	throw new InvalidStateException("Play: " + playId + ", cannot be voided in current state");
        }
        return play;
    }

    /**
     * If it’s handling a stake transaction, then the method updates the playRef (only if provided)
     * If it’s handling a win transaction, then method sets:
     *      * Win amount
     *      * Number of transactions (increment)
     *      * playRef (if provided, and existing is not null and different)
     *      * modifiedAt
     *      * status (to FINISHED)
     * @param txn
     * @param txnReceipt
     */
    @Transactional
    public void updateFromTxnReceipt(Txn txn, TxnReceipt txnReceipt) {
        final var playID = txn.getPlayId();
        Play play = playRepository.findAndLockByPlayId(playID)
                .orElseThrow(() -> new InvalidStateException("Unable to find playID " + playID + " to update."));
        if (play.getStatus() != PlayStatus.FINISHED) {
            if(txn.isStake()) {
                updatePlayRef(play, txnReceipt);
            } else if(txn.isWin()) {
                play.setWin(play.getWin().add(txn.getAmount()));
                play.setNumTxns(play.getNumTxns()+1);
                play.setModifiedAt(ZonedDateTime.now(ZoneId.of("UTC")));
                play.setStatus(PlayStatus.FINISHED);
                txn.getEvents().stream()
                        .filter(TxnCleardownEvent.class::isInstance)
                        .map(TxnCleardownEvent.class::cast)
                        .findFirst()
                        .ifPresent(cleardownEvent -> play.setCleardownAmount((cleardownEvent.getAmount())));
                updatePlayRef(play, txnReceipt);
            }
        }
    }

    private void updatePlayRef(Play play, TxnReceipt txnReceipt) {

        final var currentPlayRef = play.getPlayRef();
        final var newPlayRef = txnReceipt.getPlayRef();

        if(currentPlayRef == null && newPlayRef != null) {
            play.setPlayRef(newPlayRef);
            play.setModifiedAt(ZonedDateTime.now(ZoneId.of("UTC")));
            playRepository.save(play);
        } else if(newPlayRef != null && currentPlayRef != null && !currentPlayRef.equals(newPlayRef)) {
            throw new InvalidStateException("Different play_refs seen for the same play. Was " + currentPlayRef + " and now " + newPlayRef);
        }
    }

    @Cacheable(cacheNames = CacheConfig.GET_PLAY_CACHE_NAME, cacheManager = CacheConfig.CACHE_MANAGER_NAME)
    public Play getPlay(String playId) {
        return playRepository.findById(playId).orElseThrow(PlayNotFoundException::new);
    }

	public List<Play> getPlays(PlaySearchArguments playSearchArguments) {
		return playSearchService.search(playSearchArguments)
                .map(playSearchRecord -> {
                        final var play = new Play();
                        play.setPlayId(playSearchRecord.getPlayId());
                        play.setPlayerId(playSearchRecord.getPlayerId());
                        play.setMode(playSearchRecord.getMode());
                        play.setGameCode(playSearchRecord.getGameCode());
                        play.setGuest(playSearchRecord.isGuest());
                        play.setStatus(playSearchRecord.getStatus());
                        play.setCcyCode(playSearchRecord.getCcyCode());
                        play.setIgpCode(playSearchRecord.getIgpCode());
                        play.setCreatedAt(playSearchRecord.getCreatedAt());
                        play.setModifiedAt(playSearchRecord.getModifiedAt());
                        play.setStake(playSearchRecord.getStake());
                        play.setWin(playSearchRecord.getWin());
                        play.setNumTxns(playSearchRecord.getNumTxns());
                        play.setPlayRef(playSearchRecord.getPlayRef());
                        return play;
                })
                .getContent();
	}

    @Transactional
    public void markPlayAsAutocompleted(String playId) {
        Play play = playRepository.findById(playId).orElseThrow(PlayNotFoundException::new);
        play.setAutoCompleted(true);

        playRepository.save(play);
    }
}
