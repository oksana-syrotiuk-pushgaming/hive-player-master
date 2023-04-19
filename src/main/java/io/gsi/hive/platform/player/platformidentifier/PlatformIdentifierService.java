package io.gsi.hive.platform.player.platformidentifier;

import io.gsi.commons.exception.BadRequestException;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@AllArgsConstructor
@Component
public class PlatformIdentifierService {

  private static final Log logger = LogFactory.getLog(PlatformIdentifierService.class);

  /**
   * Platform identifier is used in txn_id, play_id, round_id to ensure globally unique identifiers.
   * It should ONLY be injected by K8's Admission Controllers. It should NEVER have a default/baked
   * in value. Or be set by another service. If the platformId is missing or malformed it should
   * error out and prevent start up of the deployment. To ensure correct format all id's should be
   * formed using functions from this class. Expected format "gameId-platformId-rawId" for txn_id,
   * play_id, round_id.
   */
  @Value("${platform.identifier}")
  private String platformIdentifier;

  @Value("${hive.platform.identifier.validation.strict.enabled:false}")
  private boolean strictValidationEnabled;


  @PostConstruct
  public void validatePlatformId() {
    if (StringUtils.isBlank(platformIdentifier)) {
      throw new IllegalArgumentException("Platform identifier not found!");
    }
    if (platformIdentifier.length() != 4) {
      throw new IllegalArgumentException("Platform identifier is not 4 characters. "
          + platformIdentifier);
    }

    logger.info("Platform Identifier initialized as " + platformIdentifier);
    if (strictValidationEnabled) {
      logger.info("Platform Identifier strict validation is enabled!");
    }
  }

  /**
   * Validation of txnRequests to ensure id's are correctly structure and match the expected
   * values.
   * <p>
   * Pg-Slot and Gsi-Slot id's have slightly different formats. This requires different logic to
   * validate gameId/platformIds depending on which engine is used and if they are new/old ids. To
   * solve this we first detect which game engine is used based on the expected gameId. pg-slot
   * games all start with '11' giving a length of 5 e.g '11xxx' gsi-slot games all start with '1'
   * giving a length of 4 e.g '1xxx' Then validate based on the expected format.
   * <p>
   * Example new/old formats:
   * <p>
   * Pre-PlatformId formats. Pg-Slot   txnID = 11xxx-yyyyy-zzzzz, playId/roundId = 11xxx-yyyyy
   * Gsi-slot  txnId = 1xxx-zzzzz, playId/roundId = 1xxx-yyyyy
   * <p>
   * New-PlatformId formats. Pg-Slot   txnID = 11xxx-pppp-yyyyy-zzzzz, playId/roundId =
   * 11xxx-pppp-yyyyy Gsi-slot  txnId = 1xxx-pppp-zzzzz, playId/roundId = 1xxx-pppp-yyyyy
   */
  public void validateTxnRequestPrefixes(TxnRequest txnRequest) {
    final var  gameId =  txnRequest.getTxnId().split("-")[0];
    if (isPgSlotGameId(gameId)) {
      validatePGSlotTxnId(txnRequest.getTxnId(), gameId, "txnId");
      validatePGSlotActionId(txnRequest.getPlayId(), gameId, "playId");
      validatePGSlotActionId(txnRequest.getRoundId(), gameId, "roundId");
    } else {
      validateGSISlotIdentifier(txnRequest.getTxnId(), gameId, "txnId");
      validateGSISlotIdentifier(txnRequest.getPlayId(), gameId, "playId");
      validateGSISlotIdentifier(txnRequest.getRoundId(), gameId, "roundId");
    }
  }

  private void validateGSISlotIdentifier(String id, String gameId, String idName) {
    String[] identifierSplit = splitIdentifier(id);

    if (identifierSplit.length == 2) {
      if (strictValidationEnabled) {
        throw new BadRequestException(idName + " is missing platform instance identifier");
      }
    } else if (identifierSplit.length == 3) {
      comparePlatformIdentifer(identifierSplit[1], idName);

    } else {
      throw new BadRequestException("Unknown gsi-slot id format! " + idName + " " + id);

    }
    compareGameIdPrefix(identifierSplit[0], gameId, idName);
  }

  private void validatePGSlotTxnId(String id, String gameId, String idName) {
    String[] identifierSplit = splitIdentifier(id);

    if (identifierSplit.length == 3) {
      if (strictValidationEnabled) {
        throw new BadRequestException(idName + " is missing platform instance identifier");
      }
    } else if (identifierSplit.length == 4) {
      comparePlatformIdentifer(identifierSplit[1], idName);

    } else {//Unknown format..
      throw new BadRequestException("Unknown pg-slot id format! " + idName + " " + id);
    }
    compareGameIdPrefix(identifierSplit[0], gameId, idName);
  }

  private void validatePGSlotActionId(String id, String gameId, String idName) {
    String[] identifierSplit = splitIdentifier(id);

    if (identifierSplit.length == 2) {
      if (strictValidationEnabled) {
        throw new BadRequestException(idName + " is missing platform instance identifier");
      }
    } else if (identifierSplit.length == 3) {
      comparePlatformIdentifer(identifierSplit[1], idName);

    } else {//Unknown format..
      throw new BadRequestException("Unknown pg-slot id format! " + idName + " " + id);
    }
    compareGameIdPrefix(identifierSplit[0], gameId, idName);
  }

  private String[] splitIdentifier(String id) {
    return id.split("-");
  }

  private boolean isPgSlotGameId(String gameId) {
    //All Pg-slot gameId start with '11' giving a length of 5. e.g 11xxx
    //Gsi-slot gameIds start with '1' giving a length of 4 e.g 1xxx
    //We're using this is differentiate between gsi-slot and pg-slot txnRequests.
    return gameId.length() == 5;
  }


  private void compareGameIdPrefix(String prefix, String gameId, String idName) {
    if (!prefix.equals(gameId)) {
      throw new BadRequestException(idName + " gameId prefix mismatch");
    }
  }

  private void comparePlatformIdentifer(String platformId, String idName) {
    if (!platformIdentifier.equals(platformId)) {
      throw new BadRequestException(
          idName + " platform identifier does not match injected value.");
    }
  }

  public String createFreeRoundsCleardownId(String rawId) {
    return ("FRCLR-" + platformIdentifier + "-" + rawId);
  }
}