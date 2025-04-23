package info.svetlik.rb.report.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import info.svetlik.rb.report.configuration.ConfigurationHolder;
import info.svetlik.rb.report.service.AttachmentInfo;
import info.svetlik.rb.report.service.ImapService;
import info.svetlik.rb.report.service.PersistenceService;
import info.svetlik.rb.report.support.event.CancellationRequestEvent;
import info.svetlik.rb.report.support.event.DownloadFinishedEvent;
import info.svetlik.rb.report.support.event.DownloadProgressEvent;
import info.svetlik.rb.report.support.event.DownloadStartedEvent;
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

	private static final String MESSAGE_ID_HEADER_NAME = "Message-ID";
	private static final String EXISTING_MESSAGES_FILE_NAME = ".existing_messages.json";
	private static final DateTimeFormatter EMAIL_DATE_FORMAT = DateTimeFormatter.ofPattern(
		    "EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	private static final String DATE_HEADER_NAME = "Date";

	private final ConfigurationHolder configurationHolder;
	private final PersistenceService persistenceService;
	private final ApplicationEventPublisher eventPublisher;

	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	private static record WithId<T>(String id, T value) {}

	@Override
	public void downloadReports(ImapCredentials imapCredentials) {
		final var props = new Properties();
		props.put("mail.store.protocol", "imaps");
		final var imapProperties = configurationHolder.configuration().imap();

		final var session = Session.getInstance(props);
		try (final var store = session.getStore()) {
			log.debug("Connecting to the IMAP server.");
			store.connect(imapProperties.host(), imapProperties.port(), imapCredentials.username(),
					imapCredentials.password());

			downloadFromImap(store);
		}
		catch (IOException | MessagingException e) {
			log.warn("Error downloading reports", e);
		}
		this.cancelled.set(false);
		eventPublisher.publishEvent(DownloadFinishedEvent.INSTANCE);
	}

	private void downloadFromImap(final Store store) throws MessagingException, IOException {
		log.debug("Downloading messages.");

		final var configuration = configurationHolder.configuration();
		final var imapProperties = configuration.imap();
		try (final var folder = store.getFolder(imapProperties.folder())) {
			folder.open(Folder.READ_ONLY);

			final var count = folder.getMessageCount();
			log.info("There are {} messages in the folder.", count);
			eventPublisher.publishEvent(new DownloadStartedEvent(count));
			log.debug("Ensuring working dir existence: {}", configuration.workingDir());
			Files.createDirectories(configuration.workingDir());

			log.debug("Loading existing messages.");
			final var existingMessages = persistenceService.readExistingMessages(EXISTING_MESSAGES_FILE_NAME);
			log.info("{} messages already downloaded.", existingMessages.size());
			final var newMessages = processMessages(folder, existingMessages.keySet());
			final var mergedMessages = new HashMap<>(existingMessages);
			mergedMessages.putAll(newMessages);
			persistenceService.writeExistingMessages(EXISTING_MESSAGES_FILE_NAME, mergedMessages);
		}
	}

	private Map<String, List<AttachmentInfo>> processMessages(final Folder folder, final Collection<String> messageIds)
			throws MessagingException {
		log.debug("Processing messages.");

		final var disctinctMessages = Stream.of(folder.getMessages())
				.map(this::extractMessageId)
				.map(this::checkCancellation)
				.filter(Objects::nonNull)
				.filter(message -> alreadyDownloaded(messageIds, message))
				.collect(Collectors.toMap(WithId::id, Function.identity(), ImapServiceImpl::detectDuplicateMessages));
		return disctinctMessages.values().stream()
				.map(this::checkCancellation)
				.filter(Objects::nonNull)
				.map(this::reportProgress)
				.map(this::multipart)
				.filter(Objects::nonNull)
				.map(this::processAttachments)
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableMap(WithId::id, WithId::value));
	}

	private boolean alreadyDownloaded(final Collection<String> messageIds, WithId<Message> message) {
		if (messageIds.contains(message.id())) {
			reportProgress(null);
			return false;
		}
		return true;
	}

	private <T> T checkCancellation(T t) {
		if (cancelled.get()) {
			return null;
		}

		return t;
	}

	private <T> T reportProgress(T t) {
		eventPublisher.publishEvent(DownloadProgressEvent.INSTANCE);
		return t;
	}

	private static WithId<Message> detectDuplicateMessages(WithId<Message> original, WithId<Message> duplicate) {
		log.warn("Message {} from {} seems to be duplicated.", original.id(), safeDateHeader(original.value()));
		return original;
	}

	private static OffsetDateTime safeDateHeader(Message message) {
		try {
			final var headers = message.getHeader(DATE_HEADER_NAME);
			if (headers.length > 0) {
				final var header = headers[0];
				return OffsetDateTime.parse(header, EMAIL_DATE_FORMAT);
			}
		}
		catch (MessagingException e) {
			log.warn("Cannot get message date.");
		}

		return null;
	}

	private WithId<Message> extractMessageId(Message message) {
		log.debug("Extracting message ID.");

		try {
			final var messageIds = message.getHeader(MESSAGE_ID_HEADER_NAME);

			if (messageIds.length > 0) {
				log.debug("Found message ID: {}", messageIds[0]);
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
		log.debug("Casting message {} as multipart.", message.id());
		try {
			final var content = message.value().getContent();
			if (content instanceof Multipart multipart) {
				log.debug("Message is multipart.");
				return new WithId<>(message.id(), multipart);
			}
		}
		catch (IOException | MessagingException e) {
			log.warn("Error parsing message {}", message.id(), e);
		}

		log.debug("Message is not multipart.");

		return null;
	}

	private WithId<List<AttachmentInfo>> processAttachments(final WithId<Multipart> multipart) {
		log.debug("Processing message '{}' attachments.", multipart.id());

		try {
			return new WithId<>(multipart.id(), IntStream.range(0, multipart.value().getCount())
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
		log.debug("Extracting attachment {} from a multipart message.", index);
		try {
			final var part = multipart.getBodyPart(index);
			if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
				log.debug("Part is an attachment.");
				return part;
			}
		}
		catch (MessagingException e) {
			log.warn("Error extracting attachment {} from {}", index, multipart, e);
		}

		log.debug("Part is not an attachment.");

		return null;
	}

	private AttachmentInfo saveAttachment(BodyPart part) {
		log.debug("Saving an attachment.");
		try {
			final var filename = part.getFileName();
			log.debug("Saving into: {}", filename);
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

	@EventListener
	public void cancel(CancellationRequestEvent event) {
		this.cancelled.set(true);
	}

}
