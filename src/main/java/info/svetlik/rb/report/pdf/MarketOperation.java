package info.svetlik.rb.report.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;

@Builder(toBuilder = true)
public record MarketOperation(OperationType operationType, LocalDate operationDate, String isin, Currency currency,
		double totalAmount, BigDecimal quantity) {

}
