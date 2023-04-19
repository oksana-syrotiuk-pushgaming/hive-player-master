package io.gsi.hive.platform.player.cleardown.report;

import static io.gsi.commons.test.json.JSONMatcher.json;
import static org.assertj.core.api.Assertions.assertThat;

import io.gsi.hive.platform.player.DomainTestBase;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class CleardownReportRecordSerialisationTests extends DomainTestBase {

    private static final String cleardownReportRecordJson = "json/hive/cleardownReportRecord.json";

    @Test
    public void okSerialisation() throws IOException
    {
        String actualJson = jsonMapper.objectToJson(
            CleardownReportRecordBuilders.defaultCleardownReportRecordBuilder().build());
        String expectedJson = getJsonMessage(cleardownReportRecordJson);
        MatcherAssert.assertThat(actualJson, json(expectedJson));
    }

    @Test
    public void okDeSerialisation() throws IOException
    {
        CleardownReportRecord expected = CleardownReportRecordBuilders.defaultCleardownReportRecordBuilder().build();
        CleardownReportRecord actual = jsonMapper.jsonToObject(getJsonMessage(cleardownReportRecordJson),
                CleardownReportRecord.class);
        assertThat(expected).isEqualTo(actual);
    }
}
