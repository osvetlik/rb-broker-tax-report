package info.svetlik.rb.report.ui.desktop.main.impl;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.desktop.main.MainController;
import javafx.application.Platform;

@Component
public class MainControllerImpl implements MainController {

	@Override
	public void onExit() {
		Platform.exit();
	}

	@Override
	public void showHome() {
	}

	@Override
	public void showDownload() {
	}

	@Override
	public void showSettings() {
	}

}
