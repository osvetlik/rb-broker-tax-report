package info.svetlik.rb.report.pdf;

import java.time.LocalDate;

public interface MarketOperation {

	LocalDate operationDate();
	String isin();

}
