package info.svetlik.rb.report.pdf.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationCurrency;
import info.svetlik.rb.report.pdf.OperationParser;
import info.svetlik.rb.report.pdf.OperationType;
import info.svetlik.rb.report.pdf.exception.UnexpectedFormatException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SaleOperationParser extends OperationParser {

	private static final String SALE_ISIN_LINE_REGEX = "^ ([A-Z]{2}[A-Z0-9]{9}\\d)\\s+";
	private static final Pattern SALE_ISIN_LINE_PATTERN = Pattern.compile(SALE_ISIN_LINE_REGEX);

	private static final String SALE_DATE_LINE_REGEX = "^Datum / Date: (.*)$";
	private static final Pattern SALE_DATE_LINE_PATTERN = Pattern.compile(SALE_DATE_LINE_REGEX);
	private static final String SALE_DATE_PATTERN = "dd.MM.yyyy";

	private static final String SALE_AMOUNTS_LINE_REGEX =
			"^((?:\\d{1,3} )*\\d+,\\d+) ((?:\\d{1,3} )*\\d+,\\d+) ((?:\\d{1,3} )*\\d+,\\d+)" // NOSONAR
			+ " (EUR|CZK|USD)"
			+ " ((?:\\d{1,3} )*\\d+,\\d+) ((?:\\d{1,3} )*\\d+,\\d+)"
			+ " (EUR|CZK|USD)"
			+ " ((?:\\d{1,3} )*\\d+,\\d+) ((?:\\d{1,3} )*\\d+,\\d+)$";
	private static final Pattern SALE_AMOUNTS_LINE_PATTERN = Pattern.compile(SALE_AMOUNTS_LINE_REGEX);

	private static final DateTimeFormatter SALE_DATE_FORMATTER = DateTimeFormatter.ofPattern(SALE_DATE_PATTERN);

	@Override
	public final OperationType operationType() {
		return OperationType.SALE;
	}

	@Override
	public MarketOperation parseInternal(BufferedReader br) throws IOException, UnexpectedFormatException, ParseException {
		final var builder = SaleInformation.builder();
		final var dateMatcher = findLineWith(br, SALE_DATE_LINE_PATTERN);
		final var operationDate = LocalDate.parse(dateMatcher.group(1), SALE_DATE_FORMATTER);
		builder.operationDate(operationDate);

		final var isinMatcher = findLineWith(br, SALE_ISIN_LINE_PATTERN);
		final var isin = isinMatcher.group(1);
		builder.isin(isin);

		final var amountsMatcher = findLineWith(br, SALE_AMOUNTS_LINE_PATTERN);
		final var quantity = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(1)).doubleValue();
		builder.quantity(quantity);
		final var unitPrice = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(2)).doubleValue();
		builder.unitPrice(unitPrice);
		final var totalAmount = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(3)).doubleValue();
		builder.totalAmount(totalAmount);
		if (totalAmount - quantity * unitPrice > AMOUNT_SANITY_CHECK) {
			log.warn("Sanity check: totalAmount({}) != quantity({}) * unitPrice({}) = {}", totalAmount, quantity,
					unitPrice, unitPrice * quantity);
		}

		final var currency = OperationCurrency.valueOf(amountsMatcher.group(4));
		builder.currency(currency);
		final var fees = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(5)).doubleValue();
		builder.fees(fees);
		final var commissions = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(6)).doubleValue();
		builder.commissions(commissions);

		final var accountCurrency = OperationCurrency.valueOf(amountsMatcher.group(7));
		if (accountCurrency != currency) {
			log.warn("Operation currency {} doesn't match account currency {}", currency, accountCurrency);
		}
		final var finalAmount = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(9)).doubleValue();
		builder.finalAmount(finalAmount);
		if (totalAmount - fees - commissions - finalAmount > AMOUNT_SANITY_CHECK) {
			log.warn("Operation final amount {} doesn't match the calculated value {} - {} - {} = {}",
					finalAmount, totalAmount, fees, commissions, totalAmount - fees - commissions);
		}

		return builder.build();
	}

}
