/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player.mesh.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import io.gsi.commons.exception.AuthorizationException;
import io.gsi.hive.platform.player.DomainTestBase;

/**
 * PlayerClientTests
 *
 */
public class MeshPlayerAuthTests extends DomainTestBase
{

	private static final String SCHEME = "Bearer";

	private static final String TOKEN = "abcdefghikl";

	private static final String HEADER = SCHEME + " " + TOKEN;

	@Test 
	public void validAuthCreated() {
		MeshPlayerAuth auth = new MeshPlayerAuth(SCHEME, TOKEN);
		assertThat(auth.valid(),is(true));
		assertThat(auth.getHeader(),is(HEADER));
	}

	@Test 
	public void validAuthCreatedFromHeader() {
		MeshPlayerAuth auth = MeshPlayerAuth.constructFromHeader(HEADER);
		assertThat(auth.valid(),is(true));
		assertThat(auth.getHeader(),is(HEADER));
	}

	@SuppressWarnings("unused")
	@Test
	public void invalidScheme() {
		thrown.expect(AuthorizationException.class);
		MeshPlayerAuth auth = new MeshPlayerAuth("NOT_A_SCHEME", TOKEN);
	}

	@Test
	public void defaultSchemeIsBearer() {
		MeshPlayerAuth auth = new MeshPlayerAuth(TOKEN);
		assertThat(auth.getType(),is(SCHEME));
	}

	@Test
	public void nullAuthenticationHeader() {
		MeshPlayerAuth auth = MeshPlayerAuth.constructFromHeader(null);
		assertThat(auth.valid(),is(false));
	}

	@Test
	public void emptyAuthenticationHeader() {
		MeshPlayerAuth auth = MeshPlayerAuth.constructFromHeader("");
		assertThat(auth.valid(),is(false));
	}

	@Test
	public void invalidSchemeInHeader() {
		String authHeader = "NOT_A_SCHEME " + TOKEN;
		thrown.expect(AuthorizationException.class);
		MeshPlayerAuth.constructFromHeader(authHeader);
	}

	@SuppressWarnings("unused")
	@Test
	public void missingTokenInHeader() {
		String authHeader = SCHEME;
		thrown.expect(AuthorizationException.class);
		MeshPlayerAuth auth = MeshPlayerAuth.constructFromHeader(authHeader);
	}

	public static MeshPlayerAuth create() {
		return new MeshPlayerAuth(SCHEME, TOKEN);
	}
}
