package io.gsi.hive.platform.player.autocompletion.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.gsi.hive.platform.player.PersistenceITBase;
import io.gsi.hive.platform.player.autocompletion.AutocompleteRequest;
import io.gsi.hive.platform.player.persistence.AutocompleteRequestRepository;
import io.gsi.hive.platform.player.presets.AutocompleteRequestPresets;
import io.gsi.hive.platform.player.presets.GamePresets;
import io.gsi.hive.platform.player.presets.SessionPresets;
import io.gsi.hive.platform.player.presets.TxnPresets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.ConstraintViolationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;

public class AutocompleteRequestRepositoryIT extends PersistenceITBase {

	@Autowired
	private AutocompleteRequestRepository autocompleteRequestRepository;

	@Test
	public void saveOk() {
		autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().build());
	}

	@Test
	public void saveAndGetOk() {
		AutocompleteRequest saved = autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().build());
		assertThat(autocompleteRequestRepository.findById(saved.getPlayId()).get()).isEqualTo(saved);
	}

	@Test
	public void findAndLockOk() {
		AutocompleteRequest saved = autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().build());

		assertThat(autocompleteRequestRepository.findAndLockByPlayId(saved.getPlayId())).isEqualTo(saved);
	}

	@Test
	public void insertToAndDeleteFromQueue() {
		AutocompleteRequest saved = autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().build());
		assertThat(autocompleteRequestRepository.findById(saved.getPlayId()).get()).isEqualTo(saved);

		autocompleteRequestRepository.deleteById(saved.getPlayId());

		assertThat(autocompleteRequestRepository.existsById(saved.getPlayId())).isFalse();
	}

	@Test(expected = ConstraintViolationException.class)
	public void insertToQueueWithNullGameCode() {
		autocompleteRequestRepository.saveAndFlush(createNullGameCodeAutocompleteRequest());
	}

	@Test(expected = JpaSystemException.class)
	public void insertToQueueWithNullPlayId() {
		autocompleteRequestRepository.saveAndFlush(createNullPlayIdAutocompleteRequest());
	}

	@Test
	public void insertDuplicateOverwritesExisting() {

		AutocompleteRequest original = new AutocompleteRequest(TxnPresets.PLAYID, GamePresets.CODE,SessionPresets.SESSIONID,true);
		AutocompleteRequest saved = autocompleteRequestRepository.saveAndFlush(original);
		AutocompleteRequest newReq = autocompleteRequestRepository.saveAndFlush(new AutocompleteRequest(TxnPresets.PLAYID, "new",SessionPresets.SESSIONID,false));

		assertThat(original).isNotEqualTo(newReq);
		assertThat(saved).isEqualTo(newReq);
		assertThat(autocompleteRequestRepository.findById(original.getPlayId()).get()).isEqualTo(newReq);
	}


	@Test
	public void getQueuedRequestsOk() {

		AutocompleteRequest one = autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().build());
		AutocompleteRequest two = autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().playId("play2").build());
		AutocompleteRequest three = autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().playId("play3").build());
		autocompleteRequestRepository.saveAndFlush(createAutocompleteRequest().playId("play4").retries(50).build());

		List<AutocompleteRequest> plays = autocompleteRequestRepository.getQueuedRequests(5, 2);

		assertThat(plays).containsExactly(one, two, three);
	}

	private AutocompleteRequest.AutocompleteRequestBuilder createAutocompleteRequest() {
		return AutocompleteRequest.builder()
				.playId(AutocompleteRequestPresets.PLAYID)
				.retries(0)
				.guest(false)
				.createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
				.gameCode(AutocompleteRequestPresets.GAMECODE)
				.sessionId(SessionPresets.SESSIONID);
	}

	private AutocompleteRequest createNullPlayIdAutocompleteRequest() {
		return AutocompleteRequest.builder()
				.playId(null)
				.gameCode(AutocompleteRequestPresets.GAMECODE)
				.retries(0)
				.guest(false)
				.createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
				.build();
	}

	private AutocompleteRequest createNullGameCodeAutocompleteRequest() {
		return AutocompleteRequest.builder()
				.playId(AutocompleteRequestPresets.PLAYID)
				.gameCode(null)
				.retries(0)
				.guest(false)
				.createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
				.build();
	}
}
