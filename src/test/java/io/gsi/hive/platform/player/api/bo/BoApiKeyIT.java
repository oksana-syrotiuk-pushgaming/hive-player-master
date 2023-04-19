package io.gsi.hive.platform.player.api.bo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;

import io.gsi.hive.platform.player.ApiITBase;
import io.gsi.hive.platform.player.presets.TxnPresets;
import io.gsi.hive.platform.player.recon.ManualReconService;
import io.gsi.hive.platform.player.txn.TxnStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "hive.player.backOffice.apiKey=backOfficeTestApiKey",
    "hive.player.backOffice.apiKey.enabled=true"
})
public class BoApiKeyIT extends ApiITBase {
    @MockBean
    private ManualReconService manualReconService;

    @Value("${hive.player.backOffice.apiKey}")
    private String backOfficeApiKeyValue;


    @Test
    public void givenBoApiKeyProvided_whenRequeueReconTxn_thenOk()
    {
        Mockito.when(manualReconService.requeueReconTxn(Mockito.anyString())).thenReturn(TxnStatus.RECON);

        RestAssured.given()
            .log().all()
            .header(BoApiKeyInterceptor.API_KEY_NAME, backOfficeApiKeyValue)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .pathParam("txnId", TxnPresets.TXNID)
            .post("/hive/bo/platform/player/v1/txn/{txnId}/recon/requeue")
            .then()
            .log().all()
            .statusCode(200)
            .body(equalTo("\"RECON\""));
    }

    @Test
    public void givenBoApiKeyNotProvided_whenRequeueReconTxn_thenAuthorizationException()
    {
        Mockito.when(manualReconService.requeueReconTxn(Mockito.anyString())).thenReturn(TxnStatus.RECON);

        RestAssured.given()
            .log().all()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .pathParam("txnId", TxnPresets.TXNID)
            .post("/hive/bo/platform/player/v1/txn/{txnId}/recon/requeue")
            .then()
            .log().all()
            .statusCode(401)
            .body(containsString("Invalid BO API Key"));
    }

    @Test
    public void givenInocorrectBoApiKeyProvided_whenRequeueReconTxn_thenAuthorizationException()
    {
        Mockito.when(manualReconService.requeueReconTxn(Mockito.anyString())).thenReturn(TxnStatus.RECON);

        RestAssured.given()
            .log().all()
            .header(BoApiKeyInterceptor.API_KEY_NAME, "IncorrectKey")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .pathParam("txnId", TxnPresets.TXNID)
            .post("/hive/bo/platform/player/v1/txn/{txnId}/recon/requeue")
            .then()
            .log().all()
            .statusCode(401)
            .body(containsString("Invalid BO API Key"));
    }

    @Test
    public void givenNoApiKeyDefined_whenStartingApplicationContext_thenApplicationContextFailsToStart() {
        ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BoApiKeyInterceptor.class)
            .withPropertyValues("hive.player.backOffice.apiKey.enabled=true",
                "hive.player.backOffice.apiKey=");

        applicationContextRunner.run((context -> {
            Assertions.assertThat(context).hasFailed();
            Assertions.assertThat(context.getStartupFailure())
                .isNotNull();
            Assertions.assertThat(context.getStartupFailure()
                .getCause().getCause()
                .getClass().getName())
                .contains("InvalidStateException");
            Assertions.assertThat(context.getStartupFailure()
                .getCause().getCause()
                .getMessage()).contains("No Back Office API key configured");
        }));
    }
}
