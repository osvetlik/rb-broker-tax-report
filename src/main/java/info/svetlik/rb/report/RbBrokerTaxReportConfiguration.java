package info.svetlik.rb.report;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;

@Configuration
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackageClasses = {
		ReporterConfigurationProperties.class,
})
public class RbBrokerTaxReportConfiguration {

}
