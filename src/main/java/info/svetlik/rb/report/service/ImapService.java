package info.svetlik.rb.report.service;

import java.io.IOException;

import info.svetlik.rb.report.ui.ImapCredentials;
import jakarta.mail.MessagingException;

public interface ImapService {

	void downloadReports(ImapCredentials imapCredentials) throws MessagingException, IOException;

}
