package info.svetlik.rb.report.pdf.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.pdf.Currency;
import info.svetlik.rb.report.pdf.FileFormat;
import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationParser;
import info.svetlik.rb.report.pdf.OperationType;
import info.svetlik.rb.report.pdf.exception.UnexpectedFormatException;

@Component
public class CertsSharesParser extends OperationParser {

	private static final String ISIN_LINE_REGEX = "^ ([A-Z]{2}[A-Z0-9]{9}\\d)\\s+";
	private static final Pattern ISIN_LINE_PATTERN = Pattern.compile(ISIN_LINE_REGEX);

	private static final String SELL_PURCHASE_LINE_REGEX = "^(sell|purchase)$";
	private static final Pattern SELL_PURCHASE_LINE_PATTERN = Pattern.compile(SELL_PURCHASE_LINE_REGEX);

	private static final String DATE_LINE_REGEX = "^Datum / Date: (.*)$";
	private static final Pattern DATE_LINE_PATTERN = Pattern.compile(DATE_LINE_REGEX);

	private static final String AMOUNTS_LINE_REGEX = tr("^GAR GAR GAR CUR GAR GAR CUR GAR GAR$");
	private static final Pattern AMOUNTS_LINE_PATTERN = Pattern.compile(AMOUNTS_LINE_REGEX);

	@Override
	public final FileFormat fileFormat() {
		return FileFormat.CERTS_SHARES;
	}

	@Override
	public MarketOperation parseInternal(BufferedReader br)
			throws IOException, UnexpectedFormatException, ParseException {
		final var builder = MarketOperation.builder();

		final var dateMatcher = findLineWith(br, DATE_LINE_PATTERN);
		final var operationDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMATTER);
		builder.operationDate(operationDate);

		final var isinMatcher = findLineWith(br, ISIN_LINE_PATTERN);
		final var isin = isinMatcher.group(1);
		builder.isin(isin);

		final var sellPurchaseMatcher = findLineWith(br, SELL_PURCHASE_LINE_PATTERN);
		final var sellPurchase = sellPurchaseMatcher.group(1);
		final var operationType = OperationType.fromOperationText(sellPurchase);
		builder.operationType(operationType);

		final var amountsMatcher = findLineWith(br, AMOUNTS_LINE_PATTERN);
		final var totalAmount = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(9)).doubleValue();
		builder.totalAmount(totalAmount);
		final var currency = Currency.valueOf(amountsMatcher.group(7));
		builder.currency(currency);
		final var quantity = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(1)).doubleValue();
		builder.quantity(quantity);

		return builder.build();
	}

}
