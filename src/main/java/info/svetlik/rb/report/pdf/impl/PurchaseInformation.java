package info.svetlik.rb.report.pdf.impl;

import java.time.LocalDate;

import info.svetlik.rb.report.pdf.MarketOperation;

public record PurchaseInformation(LocalDate operationDate, String isin) implements MarketOperation {

}
