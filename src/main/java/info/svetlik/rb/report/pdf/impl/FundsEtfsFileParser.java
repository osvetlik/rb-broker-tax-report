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
public class FundsEtfsFileParser extends OperationParser {

	private static final String BUYER_SELLER_RB = "Raiffeisenbank a.s.";
	private static final String SELL_PURCHASE_LINE_REGEX = "(.*)(" + BUYER_SELLER_RB + ")(.*)";
	private static final Pattern SELL_PURCHASE_LINE_PATTERN = Pattern.compile(SELL_PURCHASE_LINE_REGEX);

	private static final String ISIN_LINE_REGEX = "^([A-Z]{2}[A-Z0-9]{9}\\d)\\s+";
	private static final Pattern ISIN_LINE_PATTERN = Pattern.compile(ISIN_LINE_REGEX);

	private static final String QUANTITY_LINE_REGEX = tr("^GAR GAR CUR$");
	private static final Pattern QUANTITY_LINE_PATTERN = Pattern.compile(QUANTITY_LINE_REGEX);

	private static final String TOTAL_AMOUNT_FINDER_REGEX = "^.*Total Amount in Account Currency";
	private static final Pattern TOTAL_AMOUNT_FINDER_PATTERN = Pattern.compile(TOTAL_AMOUNT_FINDER_REGEX);

	private static final String TOTAL_AMOUNT_LINE_REGEX = QUANTITY_LINE_REGEX;
	private static final Pattern TOTAL_AMOUNT_LINE_PATTERN = Pattern.compile(TOTAL_AMOUNT_LINE_REGEX);

	private static final String DATE_LINE_REGEX = "^\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2} (\\d{2}\\.\\d{2}\\.\\d{4}) ";
	private static final Pattern DATE_LINE_PATTERN = Pattern.compile(DATE_LINE_REGEX);

	@Override
	public final FileFormat fileFormat() {
		return FileFormat.FUNDS_ETFS;
	}

	@Override
	protected MarketOperation parseInternal(BufferedReader br)
			throws IOException, UnexpectedFormatException, ParseException {
		final var builder = MarketOperation.builder();

		final var sellPurchaseMatcher = findLineWith(br, SELL_PURCHASE_LINE_PATTERN);
		final var operationType =
				BUYER_SELLER_RB.equals(sellPurchaseMatcher.group(1)) ? OperationType.SALE : OperationType.PURCHASE;
		builder.operationType(operationType);

		final var isinMatcher = findLineWith(br, ISIN_LINE_PATTERN);
		final var isin = isinMatcher.group(1);
		builder.isin(isin);

		final var quantityMatcher = findLineWith(br, QUANTITY_LINE_PATTERN);
		final var quantity = parseBd(quantityMatcher.group(1));
		builder.quantity(quantity);

		findLineWith(br, TOTAL_AMOUNT_FINDER_PATTERN);

		final var totalAmountMatcher = findLineWith(br, TOTAL_AMOUNT_LINE_PATTERN);
		final var totalAmount = PDF_NUMBER_FORMAT.parse(totalAmountMatcher.group(1)).doubleValue();
		builder.totalAmount(totalAmount);
		final var currency = Currency.valueOf(totalAmountMatcher.group(3));
		builder.currency(currency);

		final var dateMatcher = findLineWith(br, DATE_LINE_PATTERN);
		final var operationDate = LocalDate.parse(dateMatcher.group(1), DATE_FORMATTER);
		builder.operationDate(operationDate);

		return builder.build();
	}

}
