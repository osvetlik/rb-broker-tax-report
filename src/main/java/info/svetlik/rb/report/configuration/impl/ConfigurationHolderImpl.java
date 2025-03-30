package info.svetlik.rb.report.configuration.impl;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.configuration.ConfigurationHolderAdmin;
import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;

@Component
public class ConfigurationHolderImpl implements ConfigurationHolder, ConfigurationHolderAdmin {

	private ReporterConfigurationProperties reporterConfigurationProperties;

	@Override
	public ReporterConfigurationProperties configuration() {
		return reporterConfigurationProperties;
	}

	@Override
	public void replaceConfiguration(ReporterConfigurationProperties reporterConfigurationProperties) {
		this.reporterConfigurationProperties = reporterConfigurationProperties;
	}

}
