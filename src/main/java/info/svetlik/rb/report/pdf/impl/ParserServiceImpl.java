package info.svetlik.rb.report.pdf.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.pdf.FileFormat;
import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationParser;
import info.svetlik.rb.report.pdf.ParserService;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ParserServiceImpl implements ParserService {

	private static final OperationParser NOOP = new NoopOperationParser();
	protected static final double AMOUNT_SANITY_CHECK = 0.01;

	private final ConfigurationHolder configurationHolder;
	private final Map<FileFormat, OperationParser> operationParsers;

	private final PDFTextStripper pdfTextStripper = new PDFTextStripper();

	@Builder
	private record MarketOperationInformation(MarketOperation marketOperation, File file, String text) {}

	public ParserServiceImpl(ConfigurationHolder configurationHolder, Collection<OperationParser> operationParsers) {
		this.configurationHolder = configurationHolder;
		this.operationParsers = operationParsers.stream()
				.collect(Collectors.toMap(OperationParser::fileFormat,
						Function.identity(), (a1, a2) -> a1, () -> new EnumMap<>(FileFormat.class)));
	}

	@Override
	public NavigableMap<LocalDate, List<MarketOperation>> parse() {
		final var workDir = configurationHolder.configuration().workingDir();
		try (final var fileStream = Files.list(workDir)) {
			final var operationsInfo = fileStream
				.filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
				.map(Path::toFile)
				.map(this::extractTextFromPdf)
				.toList();

			operationsInfo.stream()
				.forEach(this::sanityCheck);

			final var result = operationsInfo.stream()
					.map(MarketOperationInformation::marketOperation)
					.filter(Objects::nonNull)
					.collect(Collectors.groupingBy(MarketOperation::operationDate,
							() -> new TreeMap<>(Comparator.naturalOrder()), Collectors.toList()));

			result.entrySet().stream()
				.forEach(this::debug);

			return result;
		} catch (IOException e) {
			log.warn("Unable to list files from {}", workDir, e);
		}

		return new TreeMap<>(); // Will be a RuntimeException instead
	}

	private MarketOperationInformation extractTextFromPdf(File pdf) {
		log.info("Processing {}", pdf.getName());
		final var fileName = pdf.getName();
		final var fileFormat = FileFormat.fromFileName(fileName);
		final var builder = MarketOperationInformation.builder();
		builder.file(pdf);

		if (fileFormat != null) {
			try (final var document = Loader.loadPDF(pdf)) {
				final String text = pdfTextStripper.getText(document);
				builder.text(text);
				final var operation = operationParsers.getOrDefault(fileFormat, NOOP).parse(text);
				log.info("Found operation: {}", operation);
				builder.marketOperation(operation);
			}
			catch (IOException e) {
				log.warn("Cannot load PDF {}", fileName, e);
			}
		}
		else {
			log.info("Unknown file type: {}", fileName);
		}

		return builder.build();
	}

	private void sanityCheck(MarketOperationInformation marketOperationInformation) {
		final var marketOperation = marketOperationInformation.marketOperation();
		if (marketOperation != null && (marketOperation.currency() == null
					|| marketOperation.isin() == null
					|| marketOperation.operationDate() == null
					|| marketOperation.operationType() == null
					|| marketOperation.totalAmount() == 0.0
					|| marketOperation.quantity() == 0.0)) {
			log.warn("Incomplete operation:\n{}\n{}\n{}", marketOperationInformation.file().getName(),
					marketOperationInformation.text(), marketOperation);
		}
	}

	private void debug(Map.Entry<LocalDate, List<MarketOperation>> marketOperations) {
		log.info("\n\n{}\n", marketOperations.getKey());
		marketOperations.getValue().stream()
			.forEach(mo -> log.info("\n{}", mo));
	}

	private static class NoopOperationParser extends OperationParser {

		@Override
		public FileFormat fileFormat() {
			return null;
		}

		@Override
		public MarketOperation parseInternal(BufferedReader br) {
			return null;
		}

	}

}
