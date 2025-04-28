package info.svetlik.rb.report.pdf.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationCurrency;
import info.svetlik.rb.report.pdf.OperationType;
import info.svetlik.rb.report.pdf.ParserService;
import info.svetlik.rb.report.pdf.exception.UnexpectedFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParserServiceImpl implements ParserService {

	private static final DecimalFormatSymbols PDF_NUMBER_FORMAT_SYMBOLS;
	private static final NumberFormat PDF_NUMBER_FORMAT;

	private static final double AMOUNT_SANITY_CHECK = 0.01;

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

	static {
		PDF_NUMBER_FORMAT_SYMBOLS = new DecimalFormatSymbols();
		PDF_NUMBER_FORMAT_SYMBOLS.setDecimalSeparator(',');
		PDF_NUMBER_FORMAT_SYMBOLS.setGroupingSeparator(' ');
		PDF_NUMBER_FORMAT = new DecimalFormat("#,##0.#", PDF_NUMBER_FORMAT_SYMBOLS);
		PDF_NUMBER_FORMAT.setGroupingUsed(true);
		PDF_NUMBER_FORMAT.setParseIntegerOnly(false);
	}

	private final ConfigurationHolder configurationHolder;

	private final PDFTextStripper pdfTextStripper = new PDFTextStripper();

	@Override
	public Map<LocalDate, List<MarketOperation>> parse() {
		final var workDir = configurationHolder.configuration().workingDir();
		try (final var fileStream = Files.list(workDir)) {
			return fileStream
				.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
				.map(Path::toFile)
				.map(this::extractTextFromPdf)
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(MarketOperation::operationDate));
		} catch (IOException e) {
			log.warn("Unable to list files from {}", workDir, e);
		}

		return Collections.emptyMap(); // Will be a RuntimeException instead
	}

	private MarketOperation extractTextFromPdf(File pdf) {
		log.info("Processing {}", pdf.getName());
		final var fileName = pdf.getName();
		final var operationType = OperationType.fromFileName(fileName);

		if (operationType != null) {
			try (final var document = Loader.loadPDF(pdf)) {
				final String text = pdfTextStripper.getText(document);
				final var operation = switch (operationType) {
				case PURCHASE -> parsePurchase(text);
				case SALE -> parseSale(text);
				default -> null;
				};
				log.info("Found operation: {}", operation);
				return operation;
			}
			catch (IOException e) {
				log.warn("Cannot load PDF {}", fileName, e);
			}
		}
		else {
			log.info("Unknown file type: {}", fileName);
		}

		return null;
	}

	private MarketOperation parseSale(String text) {
		try (
				final var sr = new StringReader(text);
				final var br = new BufferedReader(sr)) {
			log.info("\n{}", text);
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
				log.warn("Sanity check: totalAmount({}) != quantity({}) * unitPrice({}) = {}", totalAmount, quantity, unitPrice,
						unitPrice * quantity);
			}

			final var currency = OperationCurrency.valueOf(amountsMatcher.group(4));
			builder.currency(currency);
			final var fees = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(5)).doubleValue();
			builder.fees(fees);
			final var commisions = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(6)).doubleValue();
			builder.commissions(commisions);
			final var accountCurrency = OperationCurrency.valueOf(amountsMatcher.group(7));
			if (accountCurrency != currency) {
				log.warn("Operation currency {} doesn't match account currency {}", currency, accountCurrency);
			}
			final var finalAmount = PDF_NUMBER_FORMAT.parse(amountsMatcher.group(9)).doubleValue();
			builder.finalAmount(finalAmount);

			return builder.build();
		}
		catch (IOException | UnexpectedFormatException | DateTimeParseException | ParseException e) {
			log.warn("Cannot parse text:\n{}", text, e);
		}
		return null;
	}

	private MarketOperation parsePurchase(String text) {
		try (
				final var sr = new StringReader(text);
				final var br = new BufferedReader(sr)) {

		}
		catch (IOException e) {
			log.warn("Cannot parse text.", e);
		}
		return null;
	}

	private Matcher findLineWith(BufferedReader reader, Pattern pattern) throws IOException, UnexpectedFormatException {
		log.info("Loking for '{}'", pattern);
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
