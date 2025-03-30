package info.svetlik.rb.report.support.checksum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import lombok.RequiredArgsConstructor;

public interface ChecksummingStream extends AutoCloseable {

	byte[] rawDigest();

	default String hexDigest() {
		return HexFormat.of().formatHex(rawDigest());
	}

	@RequiredArgsConstructor
	enum DigestAlgorithm {
		MD5("MD5"),
		SHA1("SHA-1"),
		SHA256("SHA-256");

		private final String algorithmName;

		public MessageDigest digest() {
			try {
				return MessageDigest.getInstance(this.algorithmName);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalArgumentException("Algorithm " + algorithmName + " doesn't exist", e);
			}
		}
	}

}
