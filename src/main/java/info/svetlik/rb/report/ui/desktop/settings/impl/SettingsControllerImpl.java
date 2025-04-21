package info.svetlik.rb.report.ui.desktop.settings.impl;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.configuration.ConfigurationService;
import info.svetlik.rb.report.configuration.ReporterConfigurationProperties;
import info.svetlik.rb.report.ui.desktop.settings.SettingsController;
import info.svetlik.rb.report.ui.support.StageHolder;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettingsControllerImpl implements SettingsController, Initializable {

	private final ConfigurationService configurationService;
	private final ConfigurationHolder configurationHolder;
	private final StageHolder stageHolder;

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
		final var imap = new ReporterConfigurationProperties.ImapConfigurationProperties(imapServerField.getText(),
				Integer.valueOf(imapPortField.getText(), 10), imapFolderField.getText());
		final var configuration = new ReporterConfigurationProperties(Path.of(workingDirField.getText()), imap);
		configurationService.saveConfiguration(configuration);
	}

	@Override
	public void onReset() {
		final var configuration = configurationHolder.configuration();
		imapServerField.setText(configuration.imap().host());
		imapPortField.setText(String.valueOf(configuration.imap().port()));
		imapFolderField.setText(configuration.imap().folder());
		workingDirField.setText(String.valueOf(configuration.workingDir()));
	}

	@Override
	public void onBrowse() {
		final var directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Working dir");
		final var workingDir =  Path.of(workingDirField.getText());
		if ((workingDir != null) && Files.exists(workingDir) && Files.isDirectory(workingDir)) {
			directoryChooser.setInitialDirectory(workingDir.toFile());
		}

		final var selectedDir = directoryChooser.showDialog(stageHolder.stage());
		if (selectedDir != null) {
			workingDirField.setText(selectedDir.getAbsolutePath());
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		onReset();
	}

}
