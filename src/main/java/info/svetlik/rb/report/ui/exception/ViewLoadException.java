package info.svetlik.rb.report.ui.exception;

public class ViewLoadException extends RuntimeException {

	private static final long serialVersionUID = -7003552697301899768L;

	public ViewLoadException(String message, Throwable cause) {
		super(message, cause);
	}

	public ViewLoadException(String message) {
		super(message);
	}

	public ViewLoadException(Throwable cause) {
		super(cause);
	}

}
