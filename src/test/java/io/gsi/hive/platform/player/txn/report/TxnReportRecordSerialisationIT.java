package io.gsi.hive.platform.player.txn.report;

import io.gsi.hive.platform.player.DomainTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static io.gsi.commons.test.json.JSONMatcher.*;

public class TxnReportRecordSerialisationIT extends DomainTestBase
{
    private static final String txnReportRecordJson = "json/hive/report/txnReportRecord.json";

    @Test
    public void okSerialize() throws IOException
    {
        String expectedJson = getJsonMessage(txnReportRecordJson);
        TxnReportRecord reportRecord = TxnReportRecordBuilder.aTxnReportRecord().build();
        String actualJson = jsonMapper.objectToJson(reportRecord);
        assertThat(actualJson,json(expectedJson));
    }

    @Test
    public void okDeserialize() throws IOException
    {
        TxnReportRecord expectedTxnReportRecord = TxnReportRecordBuilder.aTxnReportRecord().build();
        TxnReportRecord actualTxnReportRecord = jsonMapper.jsonToObject(
                getJsonMessage(txnReportRecordJson), TxnReportRecord.class);
       assertThat(actualTxnReportRecord.equals(expectedTxnReportRecord),is(true));
    }
}