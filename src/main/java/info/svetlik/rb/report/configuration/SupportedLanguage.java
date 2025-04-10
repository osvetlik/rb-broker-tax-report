package info.svetlik.rb.report.configuration;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SupportedLanguage {

	CZECH(Locale.of("cs_CZ"), "Česky", "🇨🇿"),
	ENGLISH(Locale.ENGLISH, "English", "🇬🇧"),
	GERMAN(Locale.GERMAN, "Deutsch", "🇩🇪");

	private final Locale locale;
	private final String name;
	private final String flag;

}
