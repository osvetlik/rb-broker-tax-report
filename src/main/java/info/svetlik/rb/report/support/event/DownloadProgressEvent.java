package info.svetlik.rb.report.support.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DownloadProgressEvent {

	public static final DownloadProgressEvent INSTANCE = new DownloadProgressEvent();

}
