package io.gsi.hive.platform.player.play.search;

import static io.gsi.commons.test.json.JSONMatcher.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.Test;

import io.gsi.hive.platform.player.DomainTestBase;

public class PlaySearchRecordSerialisationTest extends DomainTestBase {

        private static final String playSearchRecordJson = "json/hive/playSearchRecord.json";

        @Test
        public void okSerialisation() throws IOException
        {
            String actualJson = jsonMapper.objectToJson(PlaySearchRecordBuilder.aPlaySearchRecord()
                    .build());
            String expectedJson = getJsonMessage(playSearchRecordJson);
            assertThat(actualJson, json(expectedJson));
        }

        @Test
        public void okDeSerialisation() throws IOException
        {
            PlaySearchRecord expected = PlaySearchRecordBuilder
                    .aPlaySearchRecord().build();
            PlaySearchRecord actual = jsonMapper.jsonToObject(getJsonMessage(playSearchRecordJson),
                    PlaySearchRecord.class);
            assertThat(expected.equals(actual), is(true));
        }
}
