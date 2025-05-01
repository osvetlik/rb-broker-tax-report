package info.svetlik.rb.report.pdf.impl;

import java.io.BufferedReader;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.pdf.MarketOperation;
import info.svetlik.rb.report.pdf.OperationParser;
import info.svetlik.rb.report.pdf.OperationType;

@Component
public class PurchaseOperationParser extends OperationParser {

	@Override
	public final OperationType operationType() {
		return OperationType.PURCHASE;
	}

	@Override
	protected MarketOperation parseInternal(BufferedReader br) {
		// TODO Auto-generated method stub
		return null;
	}

}
