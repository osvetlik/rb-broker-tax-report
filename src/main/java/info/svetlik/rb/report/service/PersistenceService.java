package info.svetlik.rb.report.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;

public interface PersistenceService {

	Map<String, List<AttachmentInfo>> readExistingMessages(String filename) throws IOException;
	void writeExistingMessages(String filename, Map<String, List<AttachmentInfo>> messages) throws IOException;
	String saveAttachment(final InputStream is, final String filename) throws IOException;
	ReporterConfigurationProperties readConfiguration(final Path configPath) throws IOException;
	void writeConfiguration(final Path configPath, final ReporterConfigurationProperties configurationProperties)
			throws IOException;

}
