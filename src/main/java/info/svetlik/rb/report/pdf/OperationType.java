package info.svetlik.rb.report.pdf;

import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OperationType {

	PURCHASE("purchase", 1),
	SALE("sell", -1);

	private final String operationTextValue;

	@Getter
	private final int feesCommissionsOperation;

	private final boolean matches(String value) {
		return this.operationTextValue.equals(value);
	}

	public static OperationType fromOperationText(String operationText) {
		return Stream.of(values())
				.filter(ot -> ot.matches(operationText))
				.findAny()
				.orElse(null);
	}

}
