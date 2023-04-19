package io.gsi.hive.platform.player.management;

import io.gsi.hive.platform.player.ApiITBase;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.Test;

public class ManagementEndpointsIT extends ApiITBase {

  @Test
  public void healthEndpointOk() {
    RestAssured.given()
        .accept(ContentType.JSON)
        .get("/hive/sys/health")
        .then()
        .log().all()
        .statusCode(200);
  }

  @Test
  public void prometheusEndpointOk() {

    RestAssured.given()
        .accept(ContentType.TEXT)
        .get("/hive/sys/prometheus")
        .then()
        .log().all()
        .statusCode(200);

  }

}
