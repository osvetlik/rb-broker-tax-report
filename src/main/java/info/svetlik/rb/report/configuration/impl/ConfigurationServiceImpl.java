package info.svetlik.rb.report.configuration.impl;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import info.svetlik.rb.report.configuration.ConfigurationHolderAdmin;
import info.svetlik.rb.report.configuration.ConfigurationService;
import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;
import info.svetlik.rb.report.configuration.ReporterConfigurationProperties.ImapConfigurationProperties;
import info.svetlik.rb.report.service.PersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationServiceImpl implements ConfigurationService {

	private static final String USER_HOME_SYSTEM_PROPERTY_KEY = "user.home";
	private static final String CONFIGURATION_FOLDER_NAME = ".rbb_reporter";
	private static final Path CONFIGURATION_FOLDER_PATH = Path.of(System.getProperty(USER_HOME_SYSTEM_PROPERTY_KEY),
			CONFIGURATION_FOLDER_NAME);
	private static final String CONFIGURATION_FILE_NAME = "config.json";
	private static final Path CONFIGURATION_FILE_PATH = CONFIGURATION_FOLDER_PATH.resolve(CONFIGURATION_FILE_NAME);

	private static final String DEFAULT_DATA_FOLDER_NAME = "RbbReporter";
	private static final Path DEFAULT_DATA_FOLDER_PATH = Path.of(System.getProperty(USER_HOME_SYSTEM_PROPERTY_KEY),
			DEFAULT_DATA_FOLDER_NAME);

	private final PersistenceService persistenceService;
	private final ConfigurationHolderAdmin configurationHolderAdmin;

	@PostConstruct
	public void setup() throws IOException {
		final var loaded = persistenceService.readConfiguration(CONFIGURATION_FILE_PATH);
		final var current = loaded != null
				? loaded
				: new ReporterConfigurationProperties(DEFAULT_DATA_FOLDER_PATH,
						new ImapConfigurationProperties(null, null, null));

		configurationHolderAdmin.replaceConfiguration(current);
	}

	@Override
	public boolean saveConfiguration(ReporterConfigurationProperties reporterConfigurationProperties) {
		configurationHolderAdmin.replaceConfiguration(reporterConfigurationProperties);
		try {
			persistenceService.writeConfiguration(CONFIGURATION_FILE_PATH, reporterConfigurationProperties);
			return true;
		}
		catch (IOException e) {
			log.warn("Error saving configuration", e);
		}

		return false;
	}

}
