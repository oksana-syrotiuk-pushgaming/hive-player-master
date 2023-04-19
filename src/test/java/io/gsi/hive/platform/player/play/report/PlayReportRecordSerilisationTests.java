package io.gsi.hive.platform.player.play.report;

import static io.gsi.commons.test.json.JSONMatcher.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.gsi.hive.platform.player.DomainTestBase;
import java.io.IOException;
import org.junit.Test;

public class PlayReportRecordSerilisationTests extends DomainTestBase {

    private static final String playReportRecordJson = "json/hive/playReportRecord.json";

    @Test
    public void okSerialisation() throws IOException
    {
        String actualJson = jsonMapper.objectToJson(PlayReportRecordBuilders.defaultPlayReportRecordBuilder().build());
        String expectedJson = getJsonMessage(playReportRecordJson);
        assertThat(actualJson, json(expectedJson));
    }

    @Test
    public void okDeSerialisation() throws IOException
    {
        PlayReportRecord expected = PlayReportRecordBuilders.defaultPlayReportRecordBuilder().build();
        PlayReportRecord actual = jsonMapper.jsonToObject(getJsonMessage(playReportRecordJson),
                PlayReportRecord.class);
        assertThat(expected.equals(actual), is(true));
    }
}
