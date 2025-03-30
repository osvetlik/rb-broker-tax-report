package info.svetlik.rb.report.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.service.AttachmentInfo;
import info.svetlik.rb.report.service.ImapService;
import info.svetlik.rb.report.service.PersistenceService;
import info.svetlik.rb.report.ui.ImapCredentials;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImapServiceImpl implements ImapService {

	private static final String EXISTING_MESSAGES_FILE_NAME = ".existing_messages.json";

	private final ConfigurationHolder configurationHolder;
	private final PersistenceService persistenceService;

	private static record WithId<T>(String key, T value) {}

	@Override
	public void downloadReports(ImapCredentials imapCredentials) {
		final var props = new Properties();
		props.put("mail.store.protocol", "imaps");
		final var imapProperties = configurationHolder.configuration().imap();

		final var session = Session.getInstance(props);
		try (final var store = session.getStore()) {
			store.connect(imapProperties.host(), imapProperties.port(), imapCredentials.username(),
					imapCredentials.password());

			downloadFromImap(store);
		}
		catch (IOException | MessagingException e) {
			log.warn("Error downloading reports", e);
		}
	}

	private void downloadFromImap(final Store store) throws MessagingException, IOException {
		final var configuration = configurationHolder.configuration();
		final var imapProperties = configuration.imap();
		try (final var folder = store.getFolder(imapProperties.folder())) {
			folder.open(Folder.READ_ONLY);
			Files.createDirectories(configuration.workingDir());

			final var existingMessages = persistenceService.readExistingMessages(EXISTING_MESSAGES_FILE_NAME);
			final var newMessages = processMessages(folder, existingMessages.keySet());
			final var mergedMessages = new HashMap<>(existingMessages);
			mergedMessages.putAll(newMessages);
			persistenceService.writeExistingMessages(EXISTING_MESSAGES_FILE_NAME, mergedMessages);
		}
	}

	private Map<String, List<AttachmentInfo>> processMessages(final Folder folder, final Collection<String> messageIds)
			throws MessagingException {
		return Stream.of(folder.getMessages())
				.map(this::extractMesageId)
				.filter(Objects::nonNull)
				.filter(pair -> !messageIds.contains(pair.key()))
				.map(this::multipart)
				.filter(Objects::nonNull)
				.map(this::processAttachments)
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableMap(WithId::key, WithId::value));
	}

	private WithId<Message> extractMesageId(Message message) {
		try {
			final var messageIds = message.getHeader("Message-ID");

			if (messageIds.length > 0) {
				return new WithId<>(messageIds[0], message);
			}

			log.warn("No message id");
		}
		catch (MessagingException e) {
			log.warn("Error getting message id", e);
		}

		return null;
	}

	private WithId<Multipart> multipart(WithId<Message> message) {
		try {
			final var content = message.value().getContent();
			if (content instanceof Multipart multipart) {
				return new WithId<>(message.key(), multipart);
			}
		}
		catch (IOException | MessagingException e) {
			log.warn("Error parsing message {}", message.key(), e);
		}

		return null;
	}

	private WithId<List<AttachmentInfo>> processAttachments(final WithId<Multipart> multipart) {
		try {
			return new WithId<>(multipart.key(), IntStream.of(multipart.value().getCount())
					.mapToObj(i -> extractBodyPart(multipart.value(), i))
					.filter(Objects::nonNull)
					.map(this::saveAttachment)
					.toList());
		}
		catch (MessagingException e) {
			log.warn("Cannot get part count", e);
		}

		return null;
	}

	private BodyPart extractBodyPart(final Multipart multipart, final int index) {
		try {
			final var part = multipart.getBodyPart(index);
			if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
				return part;
			}
		}
		catch (MessagingException e) {
			log.warn("Error extracting attachment {} from {}", index, multipart, e);
		}

		return null;
	}

	private AttachmentInfo saveAttachment(BodyPart part) {
		try {
			final var filename = part.getFileName();
			try (final var is = part.getInputStream()) {
				final var digest = persistenceService.saveAttachment(is, filename);
				log.info("Saved attachment: {}", filename);

				return new AttachmentInfo(filename, digest);
			}
		}
		catch (IOException | MessagingException e) {
			log.warn("Cannot save attachment", e);
		}

		return null;
	}

}
