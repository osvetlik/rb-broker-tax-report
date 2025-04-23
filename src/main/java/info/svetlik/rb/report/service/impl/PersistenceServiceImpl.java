package info.svetlik.rb.report.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;
import info.svetlik.rb.report.service.AttachmentInfo;
import info.svetlik.rb.report.service.PersistenceService;
import info.svetlik.rb.report.support.checksum.ChecksummingInputStream;
import info.svetlik.rb.report.support.checksum.ChecksummingStream.DigestAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersistenceServiceImpl implements PersistenceService {

	private static final TypeReference<Map<String, List<AttachmentInfo>>> EXISTING_MESSAGES_TYPE =
			new TypeReference<>() {};

	private final ConfigurationHolder configurationHolder;
	private final ObjectMapper objectMapper;

	@Override
	public Map<String, List<AttachmentInfo>> readExistingMessages(String filename) throws IOException {
		final var folder = configurationHolder.configuration().workingDir();
		final var storePath = folder.resolve(filename);

		if (!Files.exists(storePath)) {
			return new HashMap<>();
		}

		try (final var is = Files.newInputStream(storePath)) {
			return objectMapper.readValue(is, EXISTING_MESSAGES_TYPE);
		}
	}

	@Override
	public void writeExistingMessages(String filename, Map<String, List<AttachmentInfo>> messages) throws IOException {
		final var folder = configurationHolder.configuration().workingDir();
		Files.createDirectories(folder);
		final var storePath = folder.resolve(filename);

		try (final var os = Files.newOutputStream(storePath)) {
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, messages);
		}
	}

	@Override
	public String saveAttachment(final InputStream is, final String filename) throws IOException {
		try (final var cis = new ChecksummingInputStream(is, DigestAlgorithm.MD5)) {
			final var target = configurationHolder.configuration().workingDir().resolve(filename);
			if (Files.exists(target)) {
				log.warn("File already exists, overwriting: {}", target);
			}
			Files.copy(cis, target, StandardCopyOption.REPLACE_EXISTING);
			return cis.hexDigest();
		}
	}

	@Override
	public ReporterConfigurationProperties readConfiguration(Path configPath) throws IOException {
		if (Files.exists(configPath)) {
			try (final var is = Files.newInputStream(configPath)) {
				return objectMapper.readValue(is, ReporterConfigurationProperties.class);
			}
		}

		return null;
	}

	@Override
	public void writeConfiguration(Path configPath, ReporterConfigurationProperties configurationProperties)
			throws IOException {
		Files.createDirectories(configPath.getParent());
		try (final var os = Files.newOutputStream(configPath)) {
			objectMapper.writeValue(os, configurationProperties);
		}
	}

}
