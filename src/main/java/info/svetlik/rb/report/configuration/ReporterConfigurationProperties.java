package info.svetlik.rb.report.configuration;

import java.nio.file.Path;

import lombok.Builder;

@Builder(toBuilder = true)
public record ReporterConfigurationProperties(Path workingDir, ImapConfigurationProperties imap) {

	@Builder(toBuilder = true)
	public record ImapConfigurationProperties(String host, Integer port, String folder) {

	}

}
