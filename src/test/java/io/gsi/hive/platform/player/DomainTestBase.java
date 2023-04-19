/**
 * © gsi.io 2014
 */
package io.gsi.hive.platform.player;

import com.jayway.jsonpath.JsonPath;
import io.gsi.commons.util.JsonMapper;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Scanner;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={ObjectMapperTestConfig.class})
public abstract class DomainTestBase
{

	@Autowired
	protected JsonMapper jsonMapper;
	@Autowired
	protected Validator validator;
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	@Autowired
	private ApplicationContext ctx;

	/**
	 * Retrieves a specific JSON message resource
	 */
	public String getJsonMessage(String resourceURI) throws IOException {

		Resource jsonMsgResource = ctx.getResource(resourceURI);

		InputStream is = null;
		Scanner scanner = null;
		StringBuilder jsonMsgStr = new StringBuilder();

		is = jsonMsgResource.getInputStream();
		scanner = new Scanner(is);
		while (scanner.hasNext()) {
			jsonMsgStr.append(scanner.nextLine()).append("\n");
		}
		scanner.close();
		is.close();

		// TODO Could implement JSON validation?
		return jsonMsgStr.toString();
	}

	public Object getJsonObject(String path, String json) {
		return JsonPath.parse(json).read(path);
	}

	public String getJsonString(String path, String json) {
		return (String) JsonPath.parse(json).read(path);
	}

	public Integer getJsonInteger(String path, String json) {
		return (Integer) JsonPath.parse(json).read(path);
	}

	public BigDecimal getJsonBigDecimal(String path, String json) {
		return new BigDecimal(JsonPath.parse(json).read(path).toString());
	}

	public Double getJsonNumber(String path, String json) {
		return (Double) JsonPath.parse(json).read(path);
	}

	public Boolean getJsonBoolean(String path, String json) {
		return (Boolean) JsonPath.parse(json).read(path);
	}

	public <T> int numberOfValidationErrors(T t) {
		Set<ConstraintViolation<T>> violations =
				validator.validate(t);
		return violations.size();
	}
}
