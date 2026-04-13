package com.project.emailclient;

import org.junit.jupiter.api.Test;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MailService} content extraction.
 * No real mail server or credentials are required.
 */
class MailServiceTest {

    private static Session session() {
        return Session.getInstance(new Properties());
    }

    // ── plain text ───────────────────────────────────────────────────────────

    @Test
    void getMessageContent_simpleText_returnsBody() throws Exception {
        MimeMessage msg = new MimeMessage(session());
        msg.setText("Hello World");
        assertEquals("Hello World", MailService.getMessageContent(msg));
    }

    @Test
    void getMessageContent_emptyText_returnsEmptyString() throws Exception {
        MimeMessage msg = new MimeMessage(session());
        msg.setText("");
        final String result = MailService.getMessageContent(msg);
        assertNotNull(result);
        // Empty string is fine — caller handles blank content.
        assertTrue(result.length() <= 1);
    }

    // ── multipart: plain text preferred ──────────────────────────────────────

    @Test
    void getMessageContent_multipartAlternative_prefersPlainText() throws Exception {
        final MimeMessage parsed = roundTrip(buildAlternative(
                "Plain text content", "<b>HTML content</b>"));

        final String result = MailService.getMessageContent(parsed);
        assertTrue(result.contains("Plain text content"),
                "Should return plain text, got: " + result);
        assertFalse(result.contains("<b>"),
                "Should not return HTML when plain text is available");
    }

    // ── multipart: HTML fallback ──────────────────────────────────────────────

    @Test
    void getMessageContent_htmlOnly_returnsHtml() throws Exception {
        MimeMessage msg = new MimeMessage(session());
        MimeMultipart mp = new MimeMultipart("alternative");
        MimeBodyPart html = new MimeBodyPart();
        html.setText("<p>HTML only</p>", "UTF-8", "html");
        mp.addBodyPart(html);
        msg.setContent(mp);
        msg.saveChanges();

        final String result = MailService.getMessageContent(roundTrip(msg));
        assertTrue(result.contains("<p>HTML only</p>"),
                "Should fall back to HTML when no plain-text part exists, got: " + result);
    }

    // ── multipart: multiple plain-text parts concatenated ────────────────────

    @Test
    void getMessageContent_twoPlainParts_concatenatesBoth() throws Exception {
        MimeMessage msg = new MimeMessage(session());
        MimeMultipart mp = new MimeMultipart("mixed");
        MimeBodyPart p1 = new MimeBodyPart();
        p1.setText("Part one.", "UTF-8", "plain");
        MimeBodyPart p2 = new MimeBodyPart();
        p2.setText("Part two.", "UTF-8", "plain");
        mp.addBodyPart(p1);
        mp.addBodyPart(p2);
        msg.setContent(mp);
        msg.saveChanges();

        final String result = MailService.getMessageContent(roundTrip(msg));
        assertTrue(result.contains("Part one."), "Should contain first part");
        assertTrue(result.contains("Part two."), "Should contain second part");
    }

    // ── MessageSummary ────────────────────────────────────────────────────────

    @Test
    void messageSummary_nullsDefaultToNone() {
        MessageSummary s = new MessageSummary(null, null, null, null);
        assertEquals("[none]", s.sender);
        assertEquals("[none]", s.subject);
        assertEquals("[none]", s.date);
    }

    @Test
    void messageSummary_preservesValues() {
        MessageSummary s = new MessageSummary("alice@example.com", "Hello", "2026-04-12", null);
        assertEquals("alice@example.com", s.sender);
        assertEquals("Hello",             s.subject);
        assertEquals("2026-04-12",        s.date);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Round-trips a MimeMessage through serialization so JavaMail fully parses the
     * MIME structure. In-memory constructed messages may not behave identically to
     * messages fetched from a server; round-tripping normalizes the behavior.
     */
    private static MimeMessage roundTrip(MimeMessage msg) throws Exception {
        msg.saveChanges();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        msg.writeTo(bos);
        return new MimeMessage(session(), new ByteArrayInputStream(bos.toByteArray()));
    }

    /** Builds a multipart/alternative message with one plain-text and one HTML part. */
    private static MimeMessage buildAlternative(String plainText, String htmlText) throws Exception {
        final MimeMessage msg = new MimeMessage(session());
        final MimeMultipart mp = new MimeMultipart("alternative");

        final MimeBodyPart plain = new MimeBodyPart();
        plain.setText(plainText, "UTF-8", "plain");
        mp.addBodyPart(plain);

        final MimeBodyPart html = new MimeBodyPart();
        html.setText(htmlText, "UTF-8", "html");
        mp.addBodyPart(html);

        msg.setContent(mp);
        return msg;
    }
}
