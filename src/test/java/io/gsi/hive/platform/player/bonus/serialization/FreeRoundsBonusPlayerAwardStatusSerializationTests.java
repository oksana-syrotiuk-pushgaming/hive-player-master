package io.gsi.hive.platform.player.bonus.serialization;

import static io.gsi.commons.file.FileUtils.resourceAsString;
import static org.assertj.core.api.Assertions.assertThat;

import io.gsi.commons.util.JsonMapper;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus;
import io.gsi.hive.platform.player.bonus.award.FreeRoundsBonusPlayerAwardStatus.Status;
import io.gsi.hive.platform.player.mapper.MapperConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MapperConfig.class)
public class FreeRoundsBonusPlayerAwardStatusSerializationTests {

  @Value("classpath:json/hive/bonus/awardStatus.json")
  private Resource awardStatusJson;

  @Autowired
  private JsonMapper jsonMapper;

  @Test
  public void deserializeOk() {
    FreeRoundsBonusPlayerAwardStatus freeRoundsBonusPlayerAwardStatus = jsonMapper
        .jsonToObject(resourceAsString(awardStatusJson),
            FreeRoundsBonusPlayerAwardStatus.class);

    assertThat(freeRoundsBonusPlayerAwardStatus.getStatus()).isEqualTo(Status.active);
  }

  @Test
  public void serializeOk() {
    FreeRoundsBonusPlayerAwardStatus freeRoundsBonusPlayerAwardStatus = FreeRoundsBonusPlayerAwardStatus
        .builder().status(Status.active).build();
    String json = jsonMapper.objectToJson(freeRoundsBonusPlayerAwardStatus);
    assertThat(json).isEqualTo("{\"status\":\"active\"}");
  }

}
