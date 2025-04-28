package info.svetlik.rb.report.pdf;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ParserService {

	Map<LocalDate, List<MarketOperation>> parse();

}
