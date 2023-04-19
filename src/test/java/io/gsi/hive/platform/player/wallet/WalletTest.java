package io.gsi.hive.platform.player.wallet;

import io.gsi.hive.platform.player.bonus.wallet.BalanceFund;
import io.gsi.hive.platform.player.bonus.wallet.FreeroundsFund;
import io.gsi.hive.platform.player.bonus.wallet.OperatorFreeroundsFund;
import io.gsi.hive.platform.player.mesh.presets.MeshWalletPresets;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.gsi.hive.platform.player.txn.OperatorFreeroundsFundPresets.baseOperatorFreeroundsFund;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WalletTest {

    @Test
    public void testCopyConstructor() {
        Wallet copiedWallet = new Wallet();
        copiedWallet.setMessage(MeshWalletPresets.WALLET_MESSAGE);
        copiedWallet.setBalance(MeshWalletPresets.DEFAULT_BALANCE);

        FreeroundsFund freeroundsFund = new FreeroundsFund();
        OperatorFreeroundsFund operatorFreeroundsFund = baseOperatorFreeroundsFund().build();
        BalanceFund balanceFund = new BalanceFund(FundType.BONUS, BigDecimal.TEN);
        ArrayList<Fund> funds = new ArrayList<>();
        funds.add(freeroundsFund);
        funds.add(operatorFreeroundsFund);
        funds.add(balanceFund);

        copiedWallet.setFunds(funds);


        Wallet wallet = new Wallet(copiedWallet);
        assertEquals(wallet.getBalance(), MeshWalletPresets.DEFAULT_BALANCE);
        assertEquals(wallet.getMessage(), MeshWalletPresets.WALLET_MESSAGE);

        List<Fund> actualFunds = wallet.getFunds();
        assertNotNull(actualFunds);
        assertEquals(actualFunds.get(0), freeroundsFund);
        assertEquals(actualFunds.get(1), operatorFreeroundsFund);
        assertEquals(actualFunds.get(2), balanceFund);
    }
}