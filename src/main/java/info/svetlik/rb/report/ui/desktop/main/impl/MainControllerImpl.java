package info.svetlik.rb.report.ui.desktop.main.impl;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.desktop.download.DownloadController;
import info.svetlik.rb.report.ui.desktop.home.HomeController;
import info.svetlik.rb.report.ui.desktop.main.MainController;
import info.svetlik.rb.report.ui.desktop.settings.SettingsController;
import info.svetlik.rb.report.ui.support.StageHolderAdmin;
import info.svetlik.rb.report.ui.support.ViewRegistry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MainControllerImpl implements MainController, Initializable {

	private final ViewRegistry viewRegistry;
	private final StageHolderAdmin stageHolderAdmin;

	private Node homeView;
	private Node downloadView;
	private Node settingsView;

	private Toggle lastSelected;
	private boolean keepSelected = false;

	private Stage stage;

	@FXML
	private StackPane contentArea;

	@FXML
	private ToggleGroup sidebarGroup;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.homeView = viewRegistry.view("HomeView", HomeController.class).root();
		this.downloadView = viewRegistry.view("DownloadView", DownloadController.class).root();
		this.settingsView = viewRegistry.view("SettingsView", SettingsController.class).root();
		contentArea.getChildren().setAll(homeView);

		sidebarGroup.getToggles().stream()
			.filter(t -> ToggleButton.class.isAssignableFrom(t.getClass()))
			.map(t -> (ToggleButton) t)
			.forEach(t -> addToggleBlocker(t, sidebarGroup));

		sidebarGroup
			.selectedToggleProperty()
			.addListener((obs, oldValue, newValue) -> this.lastSelected = oldValue);
	}

	@Override
	public void onExit() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.initOwner(stage);
		alert.setTitle("Confirm Exit");
		alert.setHeaderText("Are you sure you want to exit?");
		alert.setContentText("We're gonna miss you!");

		ButtonType yes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
		ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(yes, no);

		Optional<ButtonType> result = alert.showAndWait();
		result.filter(yes::equals)
			.ifPresentOrElse(b -> Platform.exit(), () -> {
				if (!keepSelected) {
					sidebarGroup.selectToggle(this.lastSelected);
				}
			});
	}

	@Override
	public void showHome() {
		contentArea.getChildren().setAll(homeView);
	}

	@Override
	public void showDownload() {
		contentArea.getChildren().setAll(downloadView);
	}

	@Override
	public void showSettings() {
		contentArea.getChildren().setAll(settingsView);
	}

	private void addToggleBlocker(ToggleButton button, ToggleGroup group) {
		button.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (button.isSelected() && group.getSelectedToggle() == button) {
				event.consume();
			}
		});
	}

	@Override
	public void setStage(Stage stage) {
		this.stage = stage;
		stageHolderAdmin.setStage(stage);
		this.stage.centerOnScreen();
		this.stage.setMaximized(true);
		stage.setOnCloseRequest(e -> {
			e.consume();
			this.keepSelected = true;
			onExit();
			this.keepSelected = false;
		});
	}

}
