package io.gsi.hive.platform.player.txn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.hive.platform.player.mapper.MapperConfig;
import io.gsi.hive.platform.player.txn.event.TxnRequest;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.json.JacksonTester;
import static org.assertj.core.api.Assertions.assertThat;

public class TxnRequestSerializationTests {

  private JacksonTester<TxnRequest> jsonTester;

  @Before
  public void setup() {
    ObjectMapper objectMapper = new MapperConfig().objectMapper();

    JacksonTester.initFields(this, objectMapper);
  }

  @Test
  public void deserializeExtraInfoObject() throws IOException {
    TxnRequest txnRequest = jsonTester.read("/json/hive/txn/txnRequest.json").getObject();
    assertThat(txnRequest.getExtraInfo()).isNotNull();
  }
}
