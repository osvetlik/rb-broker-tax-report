package info.svetlik.rb.report.support.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CancellationRequestEvent {

	public static final CancellationRequestEvent INSTANCE = new CancellationRequestEvent();

}
