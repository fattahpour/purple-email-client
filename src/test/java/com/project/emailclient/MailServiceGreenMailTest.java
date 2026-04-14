package com.project.emailclient;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.user.GreenMailUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.mail.internet.MimeMessage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailServiceGreenMailTest {

    private static final String HOST = "127.0.0.1";
    private static final String USER = "user@example.test";
    private static final String PASSWORD = "secret";

    private GreenMail greenMail;
    private GreenMailUser user;
    private final MailService service = new MailService();

    @AfterEach
    void tearDown() {
        service.disconnect();
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Test
    void sendMessage_smtpWithoutStartTls_deliversMessage() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_SMTP);
        createUser();

        service.sendMessage(profile("imap", 143, false, smtpPort(), false),
                PASSWORD, USER, USER, "Plain SMTP", "Hello over SMTP");

        assertTrue(greenMail.waitForIncomingEmail(1));
        final MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertEquals("Plain SMTP", messages[0].getSubject());
        assertEquals("Hello over SMTP", MailService.getMessageContent(messages[0]).trim());
    }

    @Test
    void sendMessage_smtpWithStartTls_deliversMessage() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_SMTP);
        createUser();

        service.sendMessage(profile("imap", 143, false, smtpPort(), true, true),
                PASSWORD, USER, USER, "STARTTLS SMTP", "Hello over STARTTLS");

        assertTrue(greenMail.waitForIncomingEmail(1));
        final MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertEquals("STARTTLS SMTP", messages[0].getSubject());
        assertEquals("Hello over STARTTLS", MailService.getMessageContent(messages[0]).trim());
    }

    @Test
    void connect_imapWithoutSsl_readsInbox() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_IMAP);
        createUser();
        deliverMessage("Plain IMAP", "Read over IMAP");

        final List<MessageSummary> summaries = service.connect(
                profile("imap", imapPort(), false, 25, false), PASSWORD);

        assertEquals(1, summaries.size());
        assertEquals("Plain IMAP", summaries.get(0).subject);
        assertEquals("Read over IMAP", service.getContent(summaries.get(0)).trim());
    }

    @Test
    void connect_imapWithSsl_readsInbox() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_IMAPS);
        createUser();
        deliverMessage("SSL IMAP", "Read over IMAPS");

        final List<MessageSummary> summaries = service.connect(
                profile("imaps", imapsPort(), true, 25, false, true), PASSWORD);

        assertEquals(1, summaries.size());
        assertEquals("SSL IMAP", summaries.get(0).subject);
        assertEquals("Read over IMAPS", service.getContent(summaries.get(0)).trim());
    }

    @Test
    void refreshInbox_imapReadsNewlyDeliveredMail() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_IMAP);
        createUser();

        final MailProfile profile = profile("imap", imapPort(), false, 25, false);
        assertTrue(service.connect(profile, PASSWORD).isEmpty());

        deliverMessage("New IMAP Mail", "Arrived after connect");

        final List<MessageSummary> summaries = service.refreshInbox();
        assertEquals(1, summaries.size());
        assertEquals("New IMAP Mail", summaries.get(0).subject);
        assertEquals("Arrived after connect", service.getContent(summaries.get(0)).trim());
    }

    @Test
    void sendThenReceive_smtpAndImapWithoutSsl_roundTripsMessage() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_SMTP, ServerSetup.PROTOCOL_IMAP);
        createUser();

        final MailProfile profile = profile("imap", imapPort(), false, smtpPort(), false);
        service.sendMessage(profile, PASSWORD, USER, USER, "Round Trip", "Sent then received");

        assertTrue(greenMail.waitForIncomingEmail(1));
        final List<MessageSummary> summaries = service.connect(profile, PASSWORD);

        assertEquals(1, summaries.size());
        assertEquals("Round Trip", summaries.get(0).subject);
        assertEquals("Sent then received", service.getContent(summaries.get(0)).trim());
    }

    @Test
    void sendThenReceive_startTlsSmtpAndImaps_roundTripsMessage() throws Exception {
        greenMail = start(ServerSetup.PROTOCOL_SMTP, ServerSetup.PROTOCOL_IMAPS);
        createUser();

        final MailProfile profile = profile("imaps", imapsPort(), true, smtpPort(), true, true);
        service.sendMessage(profile, PASSWORD, USER, USER, "TLS Round Trip", "Secure send and receive");

        assertTrue(greenMail.waitForIncomingEmail(1));
        final List<MessageSummary> summaries = service.connect(profile, PASSWORD);

        assertEquals(1, summaries.size());
        assertEquals("TLS Round Trip", summaries.get(0).subject);
        assertEquals("Secure send and receive", service.getContent(summaries.get(0)).trim());
    }

    private GreenMail start(String... protocols) {
        final ServerSetup[] setups = new ServerSetup[protocols.length];
        for (int i = 0; i < protocols.length; i++) {
            setups[i] = new ServerSetup(0, HOST, protocols[i]);
        }
        final GreenMail mail = new GreenMail(setups);
        mail.start();
        return mail;
    }

    private void createUser() {
        user = greenMail.setUser(USER, USER, PASSWORD);
    }

    private void deliverMessage(String subject, String body) throws Exception {
        user.deliver(createMessage(subject, body));
    }

    private MimeMessage createMessage(String subject, String body) throws Exception {
        final MimeMessage message = new MimeMessage((javax.mail.Session) null);
        message.setFrom(USER);
        message.setRecipients(javax.mail.Message.RecipientType.TO, USER);
        message.setSubject(subject);
        message.setText(body);
        message.saveChanges();
        return message;
    }

    private MailProfile profile(String incomingProtocol, int incomingPort,
                                boolean incomingSsl, int smtpPort, boolean smtpStartTls) {
        return profile(incomingProtocol, incomingPort, incomingSsl, smtpPort, smtpStartTls, false);
    }

    private MailProfile profile(String incomingProtocol, int incomingPort,
                                boolean incomingSsl, int smtpPort, boolean smtpStartTls,
                                boolean trustInvalidSsl) {
        return new MailProfile("greenmail", "GreenMail", USER,
                incomingProtocol, HOST, incomingPort, incomingSsl,
                HOST, smtpPort, smtpStartTls, trustInvalidSsl);
    }

    private int smtpPort() {
        return greenMail.getSmtp().getPort();
    }

    private int imapPort() {
        return greenMail.getImap().getPort();
    }

    private int imapsPort() {
        return greenMail.getImaps().getPort();
    }
}
