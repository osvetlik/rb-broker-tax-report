package info.svetlik.rb.report.pdf.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationParser;
import info.svetlik.rb.report.pdf.OperationType;
import info.svetlik.rb.report.pdf.ParserService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ParserServiceImpl implements ParserService {

	private static final OperationParser NOOP = new NoopOperationParser();

	private final ConfigurationHolder configurationHolder;
	private final Map<OperationType, OperationParser> operationParsers;

	private final PDFTextStripper pdfTextStripper = new PDFTextStripper();

	public ParserServiceImpl(ConfigurationHolder configurationHolder, Collection<OperationParser> operationParsers) {
		this.configurationHolder = configurationHolder;
		this.operationParsers = operationParsers.stream()
				.collect(Collectors.toMap(OperationParser::operationType,
						Function.identity(), (a1, a2) -> a1, () -> new EnumMap<>(OperationType.class)));
	}

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
				final var operation = operationParsers.getOrDefault(operationType, NOOP).parse(text);
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

	private static class NoopOperationParser extends OperationParser {

		@Override
		public OperationType operationType() {
			return null;
		}

		@Override
		public MarketOperation parseInternal(BufferedReader br) {
			return null;
		}

	}

}
