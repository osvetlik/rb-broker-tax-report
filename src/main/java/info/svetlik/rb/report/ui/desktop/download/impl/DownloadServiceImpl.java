package info.svetlik.rb.report.ui.desktop.download.impl;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import info.svetlik.rb.report.service.ImapService;
import info.svetlik.rb.report.support.event.DownloadFinishedEvent;
import info.svetlik.rb.report.ui.ImapCredentials;
import info.svetlik.rb.report.ui.desktop.download.DownloadService;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

	private final ImapService imapService;
	private TaskService runningTask = null;

	@Override
	public void download(String login, String password) {
		synchronized (this) {
			if (runningTask == null) {
				runningTask = new TaskService(login, password);
				runningTask.start();
			}
		}
	}

	@RequiredArgsConstructor
	private class TaskService extends javafx.concurrent.Service<Void> {

		private final String login;
		private final String password;

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() throws Exception {
					imapService.downloadReports(new ImapCredentials(login, password));
					return null;
				}

			};
		}

	}

	@EventListener
	public void downloadFinished(DownloadFinishedEvent event) {
		synchronized (this) {
			this.runningTask = null;
		}
	}

}
