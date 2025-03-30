package info.svetlik.rb.report.ui;

import java.util.Optional;

public interface CredentialsDialog {

	Optional<ImapCredentials> askForCredentials();

}
