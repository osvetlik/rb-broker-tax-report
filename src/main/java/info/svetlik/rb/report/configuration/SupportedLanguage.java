package info.svetlik.rb.report.configuration;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SupportedLanguage {

	CZECH(Locale.of("cs", "CZ"), "Česky"),
	ENGLISH(Locale.UK, "English"),
	GERMAN(Locale.GERMANY, "Deutsch");

	public static final SupportedLanguage FALLBACK = CZECH;

	private final Locale locale;
	private final String name;

}
