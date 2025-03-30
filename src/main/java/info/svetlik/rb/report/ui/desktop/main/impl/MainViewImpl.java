package info.svetlik.rb.report.ui.desktop.main.impl;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.desktop.main.MainController;
import info.svetlik.rb.report.ui.desktop.main.MainView;
import info.svetlik.rb.report.ui.support.ViewRegistry;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MainViewImpl implements MainView {

	private final ViewRegistry viewRegistry;

	@Override
	public void start(Stage stage) {
		var view = viewRegistry.view("MainView", MainController.class);
		final var scene = new Scene(view.root(), 1024, 769);
		final var css = viewRegistry.css("sidebar");
		log.info("Style: {}", css);
		scene.getStylesheets().add(css);
		stage.setScene(scene);
		stage.setTitle("RB Broker Tax Report");
		stage.show();
	}

}
