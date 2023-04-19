package io.gsi.hive.platform.player.mesh.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsi.commons.http.endpoint.ObjectMapperHttpEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MeshEndpoint extends ObjectMapperHttpEndpoint {

	private static final String MESH_N2N_KEY = "Mesh-N2N-Key";
	private final String n2nKey;

	public MeshEndpoint(@Value("${endpoint.mesh.N2NKey}") String n2nKey,
						@Qualifier("meshRestTemplate") RestTemplate restTemplate,
						ObjectMapper objectMapper)  {
		this.n2nKey = n2nKey;
		this.setRestTemplate(restTemplate);
		this.setObjectMapper(objectMapper);
	}

	@Override
	public HttpHeaders addHeaders(HttpMethod method) {
		HttpHeaders httpHeaders =  super.addHeaders(method);
		httpHeaders.add(MESH_N2N_KEY, n2nKey);
		return httpHeaders;
	}
}
