package info.svetlik.rb.report.service.impl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import cz.cnb.api.ExratesApi;
import cz.cnb.api.model.ExRateDailyCurrencyMonthResponse;
import cz.cnb.api.model.ExRateSelectedRest;
import info.svetlik.rb.report.pdf.Currency;
import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.ParserService;
import info.svetlik.rb.report.service.TaxReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxReportServiceImpl implements TaxReportService {

	private static final List<Currency> NEED_EX_RATES = List.of(Currency.EUR, Currency.USD);

	private final ParserService parserService;
	private final ExratesApi exratesApi;

	private record RateMonthRecord(YearMonth yearMonth, Currency currency, ExRateDailyCurrencyMonthResponse exRates) {}
	private record CurrencyRates(Currency currency, List<ExRateSelectedRest> rates) {}

	@Override
	public void report() {
		final var operations = parserService.parse();
		final var czkOperations = convertToCzk(operations);

		czkOperations.values().stream()
			.flatMap(List::stream)
			.forEach(op -> log.info("\n{}", op));
	}

	private TreeMap<LocalDate, List<MarketOperation>> convertToCzk(
			final NavigableMap<LocalDate, List<MarketOperation>> operations) {
		final var necessaryMonths = operations.values().stream()
				.flatMap(List::stream)
				.filter(op -> op.currency() != Currency.CZK)
				.map(MarketOperation::operationDate)
				.map(YearMonth::from)
				.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.naturalOrder())));

		final var firstMonth = necessaryMonths.first().minusMonths(1);
		final var lastMonth = necessaryMonths.last();

		final var exRates = Stream.iterate(firstMonth, ym -> !ym.isAfter(lastMonth), ym -> ym.plusMonths(1))
				.map(this::exRates)
				.map(this::monthRates)
				.map(Map::entrySet)
				.flatMap(Set::stream)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (m1, m2) -> {
					m1.putAll(m2);
					return m1;
				}, () -> new EnumMap<>(Currency.class)));

		return operations.values().stream()
				.flatMap(List::stream).map(op -> convert(op, exRates))
				.collect(Collectors.groupingBy(MarketOperation::operationDate,
						() -> new TreeMap<>(Comparator.naturalOrder()), Collectors.toList()));
	}

	private MarketOperation convert(MarketOperation marketOperation,
			EnumMap<Currency, NavigableMap<LocalDate, ExRateSelectedRest>> rates) {
		if (marketOperation.currency() == Currency.CZK) {
			return marketOperation;
		}
		final var currency = marketOperation.currency();
		final var rate = rates.get(currency).floorEntry(marketOperation.operationDate());
		final var convertedBuilder = marketOperation.toBuilder()
				.currency(Currency.CZK)
				.totalAmount(marketOperation.totalAmount() * rate.getValue().getRate().doubleValue());
		return convertedBuilder.build();
	}

	private Map<Currency, RateMonthRecord> exRates(YearMonth yearMonth) {
		return NEED_EX_RATES.stream()
				.map(currency -> callExRateApi(currency, yearMonth))
				.collect(Collectors.toMap(RateMonthRecord::currency, Function.identity(), (a1, a2) -> a1,
						() -> new EnumMap<>(Currency.class)));
	}

	private RateMonthRecord callExRateApi(Currency currency, YearMonth yearMonth) {
		log.info("Retrieving rates for {}/{}.", currency, yearMonth);
		return new RateMonthRecord(yearMonth, currency,
				exratesApi.dailyCurrencyMonthUsingGET(currency.toString(), yearMonth.toString()));
	}

	private Map<Currency, NavigableMap<LocalDate, ExRateSelectedRest>> monthRates(
			Map<Currency, RateMonthRecord> loaded) {
		return loaded.keySet().stream()
				.map(currency -> new CurrencyRates(currency, loaded.get(currency).exRates.getRates()))
				.collect(Collectors.toMap(CurrencyRates::currency, this::expandRates, (a1, a2) -> a1,
						() -> new EnumMap<>(Currency.class)));
	}

	private NavigableMap<LocalDate, ExRateSelectedRest> expandRates(CurrencyRates currencyRates) {
		return currencyRates.rates().stream()
				.collect(Collectors.toMap(ExRateSelectedRest::getValidFor, Function.identity(), (a1, a2) -> a1,
						() -> new TreeMap<>(Comparator.naturalOrder())));
	}

}
