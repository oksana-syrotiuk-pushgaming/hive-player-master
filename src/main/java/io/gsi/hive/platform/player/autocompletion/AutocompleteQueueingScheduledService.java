package io.gsi.hive.platform.player.autocompletion;

import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.persistence.TxnRepository;
import io.gsi.hive.platform.player.play.Play;
import io.gsi.hive.platform.player.play.active.ActivePlayService;
import io.gsi.hive.platform.player.txn.Txn;
import io.gsi.hive.platform.player.txn.TxnType;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "hive.autocomplete.enabled", havingValue = "true", matchIfMissing = true)
public class AutocompleteQueueingScheduledService {
  private static final Log logger = LogFactory.getLog(AutocompleteQueueingScheduledService.class);
  private final List<ActivePlayService> activePlayService;
  private final AutocompleteRequestRepository autocompleteRequestRepository;
  private final TxnRepository txnRepository;

  public AutocompleteQueueingScheduledService(AutocompleteRequestRepository autocompleteRequestRepository,
                                              TxnRepository txnRepository,
                                              List<ActivePlayService> activePlayService) {
    this.autocompleteRequestRepository = autocompleteRequestRepository;
    this.txnRepository = txnRepository;
    this.activePlayService = activePlayService;
  }

  private void enqueuePlay(Play play){
    Txn stakeTxn = txnRepository.findByPlayIdAndTypeIn(play.getPlayId(), List.of(TxnType.STAKE, TxnType.OPFRSTK)).get(0);
    autocompleteRequestRepository.save(
        new AutocompleteRequest(play.getPlayId(), play.getGameCode(), stakeTxn.getSessionId(),play.isGuest()));
  }

  @Scheduled(initialDelayString = "${hive.autocomplete.scheduler.queue.initialDelay:60000}",
          fixedDelayString = "${hive.autocomplete.scheduler.queue.fixedDelay:1200000}")
  @Transactional //TODO - individual transactions
  public void getActivePlaysAndQueueAutocompletion() {
    logger.info("autocompletion looking for active plays to queue");
    activePlayService.stream()
        .flatMap(ActivePlayService::getActivePlays)
        .forEach(this::enqueuePlay);
  }
}