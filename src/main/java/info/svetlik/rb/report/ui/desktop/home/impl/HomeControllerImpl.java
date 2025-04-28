package info.svetlik.rb.report.ui.desktop.home.impl;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.pdf.ParserService;
import info.svetlik.rb.report.ui.desktop.home.HomeController;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HomeControllerImpl implements HomeController {

	private final ParserService parserService;

	@Override
	public void onGenerate() {
		Platform.runLater(parserService::parse);
	}

}
