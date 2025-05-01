package info.svetlik.rb.report.pdf;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;

public interface ParserService {

	NavigableMap<LocalDate, List<MarketOperation>> parse();

}
