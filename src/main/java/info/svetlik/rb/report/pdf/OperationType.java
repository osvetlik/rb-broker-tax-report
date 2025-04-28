package info.svetlik.rb.report.pdf;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum OperationType {

	PURCHASE("TCT_TRE_OT\\d+-TR\\d+-T\\d+_\\d+\\.pdf"),
	SALE("TCT_TRE_OT\\d+_\\d+\\.pdf");

	private OperationType(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	private final Pattern pattern;

	private boolean matches(String fileName) {
		return pattern.matcher(fileName).matches();
	}

	public static OperationType fromFileName(String fileName) {
		return Stream.of(values())
				.filter(ot -> ot.matches(fileName))
				.findAny()
				.orElse(null);
	}

}
