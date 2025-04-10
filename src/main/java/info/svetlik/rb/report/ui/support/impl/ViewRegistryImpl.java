package info.svetlik.rb.report.ui.support.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.exception.ViewLoadException;
import info.svetlik.rb.report.ui.support.View;
import info.svetlik.rb.report.ui.support.ViewRegistry;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ViewRegistryImpl implements ViewRegistry {

	private static final String FXML_FILES_PATH = "/ui/fxml/"; // NOSONAR: why should I?
	private static final String FXML_FILE_EXTENSION = ".xml";
	private static final String CSS_FILES_PATH = "/ui/css/"; // NOSONAR: why should I?
	private static final String CSS_FILE_EXTENSION = ".css";

	private final ConfigurableApplicationContext springContext;

	private final Map<String, URL> viewMap;
	private final Map<String, String> cssMap;

	private static record ResourcePair(String name, URL url) {}

	public ViewRegistryImpl(ResourceLoader loader, ConfigurableApplicationContext springContext) throws IOException {
		this.springContext = springContext;
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(loader);
		Resource[] viewResources = resolver.getResources("classpath:" + FXML_FILES_PATH + "*" + FXML_FILE_EXTENSION);

		this.viewMap = Stream.of(viewResources)
				.map(r -> mapResource(r, FXML_FILE_EXTENSION))
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableMap(ResourcePair::name, ResourcePair::url));

		Resource[] cssResources = resolver.getResources("classpath:" + CSS_FILES_PATH + "*" + CSS_FILE_EXTENSION);

		this.cssMap = Stream.of(cssResources)
				.map(r -> mapResource(r, CSS_FILE_EXTENSION))
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableMap(ResourcePair::name, rp -> rp.url().toExternalForm()));
	}

	private static ResourcePair mapResource(Resource resource, String extension) {
		try {
			final var url = resource.getURL();
			final var fileName = resource.getFilename();
			if (fileName == null) {
				log.warn("Cannot load a suspicious resource: {}", resource);
				return null;
			}
			final var name = fileName.substring(0, fileName.length() - extension.length());
			log.info("Found {} resource {} in {}", extension, name, fileName);
			return new ResourcePair(name, url);
		}
		catch (IOException e) {
			log.warn("Error getting resource URL: {}", resource, e);
			return null;
		}
	}

	@Override
	public <T> View<T> view(String name, Class<T> controllerClass) {
		final var url = this.viewMap.get(name);
		if (url == null) {
			throw new IllegalStateException("View not found: " + name);
		}

		final var loader = new FXMLLoader(StandardCharsets.UTF_8);
		loader.setLocation(url);
		if (controllerClass != null) {
			loader.setController(springContext.getBean(controllerClass));
		}

		try {
			final Parent root = loader.load();
			if (cssMap.containsKey(name)) {
				root.getStylesheets().add(css(name));
			}
			final T controller = loader.getController();

			return new View<>(root, controller);
		}
		catch (IOException e) {
			throw new ViewLoadException(e);
		}
	}

	@Override
	public String css(String name) {
		return cssMap.get(name);
	}

}
