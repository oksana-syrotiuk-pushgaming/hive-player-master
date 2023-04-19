package io.gsi.hive.platform.player.mesh.gateway;

import io.gsi.hive.platform.player.mesh.igpservicelocator.SupportedIgpCodes;
import io.gsi.hive.platform.player.mesh.player.MeshPlayer;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerAuth;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerClient;
import io.gsi.hive.platform.player.mesh.player.MeshPlayerWrapper;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxn;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnCancel;
import io.gsi.hive.platform.player.mesh.txn.MeshGameTxnStatus;
import io.gsi.hive.platform.player.mesh.wallet.MeshWallet;

public interface MeshGateway {

    MeshPlayerWrapper authenticate(String playerId, MeshPlayerAuth playerAuth,
                                   String rgsGameId, MeshPlayerClient client, String igpCode);

    void validateGuestLaunch(String igpCode, String authToken);

    MeshWallet getWallet(String playerId, String rgsGameId, String igpCode, MeshPlayerAuth playerAuth);

    MeshGameTxnStatus processTxn(MeshPlayerAuth playerAuth, MeshGameTxn txn, String igpCode);

    MeshGameTxnStatus cancelTxn(String rgsTxnId, MeshGameTxnCancel txnCancel, String igpCode,
                                MeshPlayerAuth meshPlayerAuth);

    MeshPlayer getPlayer(String playerId, String igpCode);
    SupportedIgpCodes getSupportedIgpCodes(String igpCode);
}
