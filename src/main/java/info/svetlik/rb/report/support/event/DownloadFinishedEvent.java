package info.svetlik.rb.report.support.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DownloadFinishedEvent {

	public static final DownloadFinishedEvent INSTANCE = new DownloadFinishedEvent();

}
