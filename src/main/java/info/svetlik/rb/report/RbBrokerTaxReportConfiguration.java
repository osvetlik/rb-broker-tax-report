package info.svetlik.rb.report;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.cnb.api.ExratesApi;
import cz.cnb.api.client.ApiClient;
import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;

@Configuration
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackageClasses = {
		ReporterConfigurationProperties.class,
})
public class RbBrokerTaxReportConfiguration {

	@Bean
	public ApiClient apiClient() {
		final var apiClient = new ApiClient();
		final var basePath = apiClient.getBasePath();
		return apiClient.setBasePath(basePath.replace("http:", "https:"));
	}

	@Bean
	public ExratesApi exratesApi(ApiClient apiClient) {
		return new ExratesApi(apiClient);
	}

}
