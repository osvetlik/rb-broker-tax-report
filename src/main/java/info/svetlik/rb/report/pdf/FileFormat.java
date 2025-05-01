package info.svetlik.rb.report.pdf;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum FileFormat {

	FUNDS_ETFS("TCT_TRE_OT\\d+-TR\\d+-T\\d+_\\d+\\.pdf"),
	CERTS_SHARES("TCT_TRE_OT\\d+_\\d+\\.pdf");

	private FileFormat(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	private final Pattern pattern;

	private boolean matches(String fileName) {
		return pattern.matcher(fileName).matches();
	}

	public static FileFormat fromFileName(String fileName) {
		return Stream.of(values())
				.filter(ot -> ot.matches(fileName))
				.findAny()
				.orElse(null);
	}

}
