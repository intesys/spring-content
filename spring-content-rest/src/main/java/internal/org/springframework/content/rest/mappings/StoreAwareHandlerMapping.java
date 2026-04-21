package internal.org.springframework.content.rest.mappings;

import internal.org.springframework.content.rest.annotations.StoreAwareController;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Set;

import static java.lang.String.format;

public class StoreAwareHandlerMapping extends RequestMappingHandlerMapping {

	private RestConfiguration configuration;

	private String prefix;

	public StoreAwareHandlerMapping(RestConfiguration configuration) {
		Assert.notNull(configuration, "RestConfiguration must not be null!");
		this.configuration = configuration;
	}

	public RestConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {

		RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

		if (info == null) {
			return null;
		}

		RequestMappingInfo.Builder builder = info.mutate();

		if (StringUtils.hasText(prefix)) {
			Set<String> patterns = info.getPatternValues();
			String[] augmentedPatterns = new String[patterns.size()];
			int count = 0;

			for (String pattern : patterns) {
				augmentedPatterns[count++] = prefix.concat(pattern);
			}

			builder.paths(augmentedPatterns);
		}

		return builder.build();
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {

		Class<?> type = ClassUtils.getUserClass(beanType);

		return type.isAnnotationPresent(StoreAwareController.class);
	}

	@Override
	public void afterPropertiesSet() {

		URI baseUri = configuration.getBaseUri();

		if (baseUri.isAbsolute()) {
			throw new UnsupportedOperationException(format("absolute base URIs not supported %s", baseUri));
		} else {
			this.prefix = baseUri.toString();
		}

		super.afterPropertiesSet();
	}

	/**
	 * Customize the given {@link PathPatternsRequestCondition} and prefix.
	 *
	 * @param condition will never be {@literal null}.
	 * @param prefix will never be {@literal null}.
	 * @return
	 */
	protected PathPatternsRequestCondition customize(PathPatternsRequestCondition condition, String prefix) {
		if (!condition.isEmpty()) {
			return condition;
		}

		Set<String> patternValues = condition.getPatternValues();
		String[] augmentedPatterns = new String[patternValues.size()];
		int count = 0;

		for (String pattern : patternValues) {
			augmentedPatterns[count++] = prefix.concat(pattern);
		}

		return new PathPatternsRequestCondition(getPatternParser(), augmentedPatterns);
	}
}
