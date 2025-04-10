package info.svetlik.rb.report.ui.desktop.settings.impl;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.desktop.settings.SettingsController;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

@Component
public class SettingsControllerImpl implements SettingsController {

	@FXML
	private TextField imapServerField;

	@FXML
	private TextField imapPortField;

	@FXML
	private TextField imapFolderField;

	@FXML
	private TextField workingDirField;

	@Override
	public void onSave() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onReset() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBrowse() {
		// TODO Auto-generated method stub

	}

}
