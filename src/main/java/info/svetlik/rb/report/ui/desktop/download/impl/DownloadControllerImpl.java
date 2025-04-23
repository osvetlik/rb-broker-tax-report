package info.svetlik.rb.report.ui.desktop.download.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import info.svetlik.rb.report.support.event.CancellationRequestEvent;
import info.svetlik.rb.report.support.event.DownloadFinishedEvent;
import info.svetlik.rb.report.support.event.DownloadProgressEvent;
import info.svetlik.rb.report.support.event.DownloadStartedEvent;
import info.svetlik.rb.report.ui.desktop.download.DownloadController;
import info.svetlik.rb.report.ui.desktop.download.DownloadService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DownloadControllerImpl implements DownloadController {

	private final DownloadService downloadService;
	private final ApplicationEventPublisher eventPublisher;

	private final AtomicInteger messageCount = new AtomicInteger(0);
	private final AtomicInteger progressCount = new AtomicInteger(0);

	@FXML
	private ProgressBar downloadProgress;

	@FXML
	private TextField loginField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Button downloadButton;

	@FXML
	private Button cancelButton;

	@Override
	public void onDownload() {
		this.progressCount.set(0);
		Platform.runLater(() -> {
			this.downloadButton.setDisable(true);
			this.cancelButton.setDisable(false);
		});
		this.downloadService.download(loginField.getText(), passwordField.getText());
	}

	@Override
	public void onCancel() {
		Platform.runLater(() -> this.cancelButton.setDisable(true));
		this.eventPublisher.publishEvent(CancellationRequestEvent.INSTANCE);
	}

	@EventListener
	public void downloadStarted(DownloadStartedEvent event) {
		final var total = event.total();
		this.messageCount.set(total);
		if (total > 0) {
			updateProgressBar(0);
		}
	}

	@EventListener
	public void downloadProgress(DownloadProgressEvent event) {
		updateProgressBar(this.progressCount.incrementAndGet());
	}

	private void updateProgressBar(int progress) {
		double total = this.messageCount.doubleValue();
		if (progress <= total) {
			Platform.runLater(() -> this.downloadProgress.setProgress(progress / total));
		}
	}

	@EventListener
	public void downloadFinished(DownloadFinishedEvent event) {
		Platform.runLater(() -> {
			this.downloadButton.setDisable(false);
			this.cancelButton.setDisable(true);
		});
	}

}
