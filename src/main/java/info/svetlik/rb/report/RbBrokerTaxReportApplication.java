package info.svetlik.rb.report;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import info.svetlik.rb.report.ui.RbBrokerTaxReportUi;
import javafx.application.Application;

@SpringBootApplication
public class RbBrokerTaxReportApplication {

	public static void main(String[] args) {
		Application.launch(RbBrokerTaxReportUi.class, args);
	}

}
