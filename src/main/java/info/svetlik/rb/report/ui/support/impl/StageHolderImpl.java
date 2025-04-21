package info.svetlik.rb.report.ui.support.impl;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.support.StageHolder;
import info.svetlik.rb.report.ui.support.StageHolderAdmin;
import javafx.stage.Stage;
import lombok.Setter;

@Component
public class StageHolderImpl implements StageHolder, StageHolderAdmin {

	@Setter
	private Stage stage;

	@Override
	public Stage stage() {
		return this.stage;
	}

}
