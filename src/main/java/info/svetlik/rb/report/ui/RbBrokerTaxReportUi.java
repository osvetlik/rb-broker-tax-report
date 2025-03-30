package info.svetlik.rb.report.ui;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import info.svetlik.rb.report.RbBrokerTaxReportApplication;
import info.svetlik.rb.report.ui.desktop.main.MainView;
import javafx.application.Application;
import javafx.stage.Stage;

public class RbBrokerTaxReportUi extends Application {

	private ConfigurableApplicationContext springContext;

	@Override
	public void init() {
		this.springContext = new SpringApplicationBuilder(RbBrokerTaxReportApplication.class)
				.run(getParameters().getRaw().toArray(String[]::new));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		final var mainView = this.springContext.getBean(MainView.class);
		mainView.start(primaryStage);
	}

	@Override
	public void stop() {
		springContext.stop();
	}

}
