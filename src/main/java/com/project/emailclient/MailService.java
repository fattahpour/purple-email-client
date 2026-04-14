package com.project.emailclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * All JavaMail operations for the Compose UI.
 *
 * <p>Maintains a single open {@link Store}/{@link Folder} session per connection.
 * Call {@link #disconnect()} before reconnecting or on app exit.
 * This class is not thread-safe: callers must serialize access (e.g., via coroutines
 * dispatched on a single thread).
 *
 * <p>Passwords are never logged or stored beyond this class instance.
 */
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    private Store  activeStore;
    private Folder activeFolder;

    // ── connect ──────────────────────────────────────────────────────────────

    /**
     * Opens an INBOX session for the given profile and returns a list of message
     * summaries in reverse arrival order (newest first).
     *
     * @param profile  the saved connection profile
     * @param password the session password (never persisted)
     */
    public List<MessageSummary> connect(MailProfile profile, String password)
            throws MessagingException {
        disconnect();

        final String proto  = profile.getIncomingProtocol();
        final String prefix = propPrefix(proto);
        final Properties props = new Properties();
        props.put("mail." + prefix + ".host", profile.getIncomingHost());
        props.put("mail." + prefix + ".port", String.valueOf(profile.getIncomingPort()));
        if (profile.isIncomingSsl()) {
            props.put("mail." + prefix + ".ssl.enable", "true");
            if (profile.isTrustInvalidSsl()) {
                // Opt-in for self-signed or otherwise invalid certificates.
                props.put("mail." + prefix + ".ssl.trust", "*");
            }
        }

        final Session session = Session.getInstance(props);
        activeStore = session.getStore(proto);
        activeStore.connect(profile.getIncomingHost(), profile.getIncomingPort(),
                profile.getUsername(), password);
        activeFolder = activeStore.getFolder("INBOX");
        activeFolder.open(Folder.READ_WRITE);

        final Message[] messages = activeFolder.getMessages();
        final FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        activeFolder.fetch(messages, fp);

        final List<MessageSummary> summaries = new ArrayList<>(messages.length);
        for (int i = messages.length - 1; i >= 0; i--) {
            summaries.add(toSummary(messages[i]));
        }
        return summaries;
    }

    // ── refresh ──────────────────────────────────────────────────────────────

    /**
     * Re-fetches all INBOX messages using the existing open connection.
     * Closes and reopens the folder so both IMAP and POP3 see newly arrived mail.
     * Returns summaries in reverse arrival order (newest first).
     *
     * @throws MessagingException if not currently connected
     */
    public List<MessageSummary> refreshInbox() throws MessagingException {
        if (activeStore == null || !activeStore.isConnected()) {
            throw new MessagingException("Not connected");
        }
        if (activeFolder != null && activeFolder.isOpen()) {
            activeFolder.close(false);
        }
        activeFolder = activeStore.getFolder("INBOX");
        activeFolder.open(Folder.READ_WRITE);

        final Message[] messages = activeFolder.getMessages();
        final FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        activeFolder.fetch(messages, fp);

        final List<MessageSummary> summaries = new ArrayList<>(messages.length);
        for (int i = messages.length - 1; i >= 0; i--) {
            summaries.add(toSummary(messages[i]));
        }
        return summaries;
    }

    // ── content ──────────────────────────────────────────────────────────────

    /**
     * Returns the plain-text content of the given message summary, falling back
     * to HTML when no plain-text part is available.
     */
    public String getContent(MessageSummary summary) throws Exception {
        return getMessageContent(summary.getMessage());
    }

    /**
     * Extracts displayable content from a {@link Message}, preferring plain text.
     * Falls back to HTML parts when no plain-text part is found.
     */
    public static String getMessageContent(Message message) throws Exception {
        final Object content = message.getContent();
        if (content instanceof Multipart multipart) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                final Part part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    sb.append(part.getContent());
                }
            }
            if (sb.length() == 0) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    final Part part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/html")) {
                        sb.append(part.getContent());
                    }
                }
            }
            return sb.toString();
        }
        return content != null ? content.toString() : "";
    }

    // ── delete ───────────────────────────────────────────────────────────────

    /**
     * Marks the given message as deleted and expunges it from the folder.
     */
    public void deleteMessage(MessageSummary summary) throws MessagingException {
        summary.getMessage().setFlag(Flags.Flag.DELETED, true);
        if (activeFolder != null) {
            activeFolder.close(true);  // expunge deleted messages
            activeFolder.open(Folder.READ_WRITE);
        }
    }

    // ── send ─────────────────────────────────────────────────────────────────

    /**
     * Sends a plain-text message via the profile's SMTP settings.
     *
     * @param password session password — not logged or retained beyond this call
     */
    public void sendMessage(MailProfile profile, String password,
                            String from, String to, String subject, String body)
            throws MessagingException {
        final Properties props = new Properties();
        props.put("mail.smtp.host", profile.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(profile.getSmtpPort()));
        props.put("mail.smtp.auth", "true");
        if (profile.isSmtpStartTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            if (profile.isTrustInvalidSsl()) {
                // Opt-in for self-signed or otherwise invalid certificates.
                props.put("mail.smtp.ssl.trust", "*");
            }
        }

        final String smtpUser = profile.getUsername();
        // Password captured in final local — lambda-safe, never stored in a field.
        final String smtpPass = password;

        final Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPass);
            }
        });

        final MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);

        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(profile.getSmtpHost(), smtpUser, smtpPass);
            transport.sendMessage(msg, msg.getAllRecipients());
        }
    }

    // ── disconnect ───────────────────────────────────────────────────────────

    /**
     * Closes the active folder and store, if any. Safe to call multiple times.
     */
    public void disconnect() {
        try {
            if (activeFolder != null && activeFolder.isOpen()) {
                activeFolder.close(false);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error closing mail folder", e);
        }
        try {
            if (activeStore != null && activeStore.isConnected()) {
                activeStore.close();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error closing mail store", e);
        }
        activeFolder = null;
        activeStore  = null;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * JavaMail property prefix. SSL protocols have their own property namespace
     * (for example {@code mail.imaps.*}), so keep the exact protocol family.
     */
    private static String propPrefix(String protocol) {
        if (protocol != null && protocol.startsWith("imaps")) return "imaps";
        if (protocol != null && protocol.startsWith("imap")) return "imap";
        if (protocol != null && protocol.startsWith("pop3s")) return "pop3s";
        return "pop3";
    }

    private static MessageSummary toSummary(Message message) {
        String sender, subject, date;
        try {
            final Address[] from = message.getFrom();
            sender = (from != null && from.length > 0) ? from[0].toString() : null;
        } catch (Exception e) { sender = null; }
        try {
            subject = message.getSubject();
        } catch (Exception e) { subject = null; }
        try {
            final Date d = message.getSentDate();
            date = d != null ? d.toString() : null;
        } catch (Exception e) { date = null; }
        return new MessageSummary(sender, subject, date, message);
    }
}
