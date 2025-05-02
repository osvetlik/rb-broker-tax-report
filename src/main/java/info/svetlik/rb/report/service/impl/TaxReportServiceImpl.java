package info.svetlik.rb.report.service.impl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
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
import info.svetlik.rb.report.pdf.OperationType;
import info.svetlik.rb.report.pdf.ParserService;
import info.svetlik.rb.report.service.TaxReportService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxReportServiceImpl implements TaxReportService {

	private static final List<Currency> NEED_EX_RATES = List.of(Currency.EUR, Currency.USD);
	private static final MathContext FOR_DIVISION = new MathContext(10, RoundingMode.HALF_UP);

	private final ParserService parserService;
	private final ExratesApi exratesApi;

	private record RateMonthRecord(YearMonth yearMonth, Currency currency, ExRateDailyCurrencyMonthResponse exRates) {}
	private record CurrencyRates(Currency currency, List<ExRateSelectedRest> rates) {}
	private record PurchasesSales(List<MarketOperation> purchases, List<MarketOperation> sales) {}

	@Builder(toBuilder = true)
	private record InvestmentResults(Year year, String isin, double purchasedFor, double soldFor) {}

	@Override
	public void report() {
		final var operations = parserService.parse();
		final var czkOperations = convertToCzk(operations);
		log.info("Reporting after CZK conversion.");
		czkOperations.values().stream()
			.forEach(this::reportCzk);

		final var purchasesSales = splitToPurchasesAndSales(czkOperations);
		final var sales = purchasesSales.sales();
		log.info("Reporting just sales.");
		reportCzk(sales);
		final var purchases = organizeByIsin(purchasesSales.purchases());

		final var results = new TreeMap<Year, Map<String, InvestmentResults>>(Comparator.naturalOrder());
		results.putAll(analyzeResults(purchases, sales));

		results.entrySet().stream()
			.forEach(this::reportYear);
	}

	private void reportCzk(List<MarketOperation> operations) {
		operations.forEach(op -> log.info("\n{}", op));
	}

	private void reportYear(Map.Entry<Year, Map<String, InvestmentResults>> yearResultsEntry) {
		log.info("Year: {}", yearResultsEntry.getKey());
		yearResultsEntry.getValue().values().forEach(this::reportInvestmentResults);
		final var yearSummary = yearResultsEntry.getValue().values().stream()
				.collect(Collectors.reducing(this::sum)) // This effectively destroys the data, only amounts stay valid
				.get();
		log.info("\nTotal for {}      \tpurchased for: {}\tsold for: {}\tresult: {}", yearResultsEntry.getKey(),
				yearSummary.purchasedFor(), yearSummary.soldFor(),
				yearSummary.soldFor() - yearSummary.purchasedFor());
	}

	private void reportInvestmentResults(InvestmentResults investmentResults) {
		log.info("\nISIN: {}\tpurchased for: {}\tsold for: {}\tresult: {}", investmentResults.isin(),
				investmentResults.purchasedFor(), investmentResults.soldFor(),
				investmentResults.soldFor() - investmentResults.purchasedFor());
	}

	private Map<Year, Map<String, InvestmentResults>> analyzeResults(
			Map<String, NavigableMap<LocalDate, MarketOperation>> purchases, List<MarketOperation> sales) {
		return sales.stream()
				.map(sale -> sell(purchases.get(sale.isin()), sale))
				.collect(Collectors.groupingBy(InvestmentResults::year, Collectors.groupingBy(InvestmentResults::isin,
						Collectors.collectingAndThen(Collectors.reducing(this::sum), Optional::get))));
	}

	private InvestmentResults sum(InvestmentResults ir1, InvestmentResults ir2) {
		return ir1.toBuilder()
				.purchasedFor(ir1.purchasedFor() + ir2.purchasedFor())
				.soldFor(ir1.soldFor() + ir2.soldFor())
				.build();
	}

	private InvestmentResults sell(NavigableMap<LocalDate, MarketOperation> purchases, MarketOperation sale) {
		var remainingQuantity = sale.quantity();
		var purchasedFor = 0.0;
		while (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
			final var purchase = purchases.pollFirstEntry().getValue();
			// Sanity checks
			if (purchase == null || purchase.operationDate().isAfter(sale.operationDate())) {
				throw new IllegalStateException();
			}
			final var purchased = purchase.quantity();
			if (purchased.compareTo(remainingQuantity) > 0) {
				final var ratio = remainingQuantity.divide(purchased, FOR_DIVISION).doubleValue();
				purchasedFor += ratio * purchase.totalAmount();

				final var remainingPurchase = purchase.toBuilder()
						.quantity(purchase.quantity().subtract(remainingQuantity))
						.totalAmount(purchase.totalAmount() - purchasedFor)
						.build();
				remainingQuantity = BigDecimal.ZERO;
				purchases.put(remainingPurchase.operationDate(), remainingPurchase);
			}
			else {
				remainingQuantity = remainingQuantity.subtract(purchased);
				purchasedFor += purchase.totalAmount();
			}
		}

		return new InvestmentResults(Year.of(sale.operationDate().getYear()), sale.isin(), purchasedFor,
				sale.totalAmount());
	}

	private PurchasesSales splitToPurchasesAndSales(NavigableMap<LocalDate, List<MarketOperation>> operations) {
		final var split = operations.values().stream()
				.flatMap(List::stream)
				.collect(Collectors.groupingBy(MarketOperation::operationType));
		return new PurchasesSales(split.getOrDefault(OperationType.PURCHASE, Collections.emptyList()),
				split.getOrDefault(OperationType.SALE, Collections.emptyList()));
	}

	private Map<String, NavigableMap<LocalDate, MarketOperation>> organizeByIsin(
			List<MarketOperation> purchases) {
		return purchases.stream()
				.collect(Collectors.groupingBy(MarketOperation::isin,
						Collectors.groupingBy(MarketOperation::operationDate,
								() -> new TreeMap<>(Comparator.naturalOrder()),
								Collectors.collectingAndThen(Collectors.reducing(this::sum), Optional::get))));
	}

	private MarketOperation sum(MarketOperation mo1, MarketOperation mo2) {
		return mo1.toBuilder()
				.totalAmount(mo1.totalAmount() + mo2.totalAmount())
				.quantity(mo1.quantity().add(mo2.quantity()))
				.build();
	}

	private NavigableMap<LocalDate, List<MarketOperation>> convertToCzk(
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
