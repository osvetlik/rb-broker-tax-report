package info.svetlik.rb.report.ui.desktop.main.impl;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import info.svetlik.rb.report.ui.desktop.main.MainController;
import info.svetlik.rb.report.ui.desktop.main.MainView;
import info.svetlik.rb.report.ui.support.ViewRegistry;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MainViewImpl implements MainView {

	private final ViewRegistry viewRegistry;

	@Override
	public void start(Stage stage) {
		var view = viewRegistry.view("MainView", MainController.class);
		final var scene = new Scene(view.root(), 1024, 769);
		final var css = viewRegistry.css("global");
		Assert.notNull(css, "Cannot find global css file");
		scene.getStylesheets().add(css);
		stage.setScene(scene);
		stage.setTitle("RB Broker Tax Report");

		final var controller = view.controller();
		controller.setStage(stage);
		stage.show();
	}

}
