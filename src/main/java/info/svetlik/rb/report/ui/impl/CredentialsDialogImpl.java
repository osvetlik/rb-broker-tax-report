package info.svetlik.rb.report.ui.impl;

import java.awt.GridLayout;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.springframework.stereotype.Component;

import info.svetlik.rb.report.ui.CredentialsDialog;
import info.svetlik.rb.report.ui.ImapCredentials;

@Component
public class CredentialsDialogImpl implements CredentialsDialog {

	@Override
	public Optional<ImapCredentials> askForCredentials() {
		JPanel panel = new JPanel(new GridLayout(2, 2));
		JTextField usernameField = new JTextField();
		JPasswordField passwordField = new JPasswordField();

		panel.add(new JLabel("Email Username:"));
		panel.add(usernameField);
		panel.add(new JLabel("Email Password:"));
		panel.add(passwordField);

		int result = JOptionPane.showConfirmDialog(null, panel, "Enter Email Credentials", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			return Optional.of(new ImapCredentials(usernameField.getText(), new String(passwordField.getPassword())));
		}

		return Optional.empty();
	}

}
