package info.svetlik.rb.report.ui.desktop.main;

import javafx.fxml.FXML;
import javafx.stage.Stage;

public interface MainController {

	@FXML
	void onExit();

	@FXML
	void showHome();

	@FXML
	void showDownload();

	@FXML
	void showSettings();

	void setStage(Stage stage);

}
