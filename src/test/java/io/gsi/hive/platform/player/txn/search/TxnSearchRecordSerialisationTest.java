package io.gsi.hive.platform.player.txn.search;

import io.gsi.hive.platform.player.DomainTestBase;
import org.junit.Test;

import java.io.IOException;

import static io.gsi.commons.test.json.JSONMatcher.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TxnSearchRecordSerialisationTest extends DomainTestBase
{
    private static final String txnSearchRecordJson = "json/hive/txnSearchRecord.json";

    @Test
    public void okSerialisation() throws IOException
    {
        String actualJson = jsonMapper.objectToJson(TxnSearchRecordBuilder
                .aTxnSearchRecord().build());
        String expectedJson = getJsonMessage(txnSearchRecordJson);
        assertThat(actualJson, json(expectedJson));
    }

    @Test
    public void okDeSerialisation() throws IOException
    {
        TxnSearchRecord expected = TxnSearchRecordBuilder
                .aTxnSearchRecord().build();
        TxnSearchRecord actual = jsonMapper.jsonToObject(getJsonMessage(txnSearchRecordJson),
                TxnSearchRecord.class);
        assertThat(expected.equals(actual), is(true));
    }
}
