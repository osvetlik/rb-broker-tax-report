package info.svetlik.rb.report.pdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.svetlik.rb.report.pdf.exception.UnexpectedFormatException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class OperationParser {

	/**
	 * Generic Amount Regular Expression
	 */
	protected static final String GAR = "((?:\\d{1,3} )*\\d+,\\d+)";
	protected static final String CUR = "(EUR|CZK|USD)";

	protected static final String DATE_PATTERN = "dd.MM.yyyy";
	protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

	protected static final DecimalFormatSymbols PDF_NUMBER_FORMAT_SYMBOLS;
	protected static final NumberFormat PDF_NUMBER_FORMAT;

	static {
		PDF_NUMBER_FORMAT_SYMBOLS = new DecimalFormatSymbols();
		PDF_NUMBER_FORMAT_SYMBOLS.setDecimalSeparator(',');
		PDF_NUMBER_FORMAT_SYMBOLS.setGroupingSeparator(' ');
		PDF_NUMBER_FORMAT = new DecimalFormat("#,##0.#", PDF_NUMBER_FORMAT_SYMBOLS);
		PDF_NUMBER_FORMAT.setGroupingUsed(true);
		PDF_NUMBER_FORMAT.setParseIntegerOnly(false);
	}

	/**
	 * Transfer component string to regex (replace GAR and CUR components with their patterns).
	 */
	protected static String tr(String regex) {
		return regex.replace("GAR", GAR).replace("CUR", CUR);
	}

	public abstract FileFormat fileFormat();

	protected abstract MarketOperation parseInternal(BufferedReader br)
			throws IOException, UnexpectedFormatException, ParseException;

	public final MarketOperation parse(String text) {
		try (
				final var sr = new StringReader(text);
				final var br = new BufferedReader(sr)) {
			return parseInternal(br);
		}
		catch (IOException | UnexpectedFormatException | DateTimeParseException | ParseException e) {
			log.warn("Cannot parse text:\n{}", text, e);
		}
		return null;
	}

	protected Matcher findLineWith(BufferedReader reader, Pattern pattern) throws IOException, UnexpectedFormatException {
		log.debug("Loking for '{}'", pattern);
		String line;
		while ((line = reader.readLine()) != null) {
			final var matcher = pattern.matcher(line);
			if (matcher.lookingAt()) {
				return matcher;
			}
		}

		throw new UnexpectedFormatException();
	}

}
