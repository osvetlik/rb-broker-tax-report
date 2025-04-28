package info.svetlik.rb.report.pdf.impl;

import java.time.LocalDate;

import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationCurrency;
import lombok.Builder;

@Builder(toBuilder = true)
public record SaleInformation(LocalDate operationDate, String isin, double quantity, double unitPrice,
		double totalAmount, OperationCurrency currency, double fees, double commissions, double finalAmount)
		implements MarketOperation {

}
