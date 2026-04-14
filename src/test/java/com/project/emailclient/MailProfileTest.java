package com.project.emailclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailProfileTest {

    private static MailProfile valid() {
        return new MailProfile(null, "Work Gmail", "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true,
                "smtp.gmail.com", 587, true);
    }

    // ── validation: valid profile ────────────────────────────────────────────

    @Test
    void validProfile_hasNoErrors() {
        assertTrue(valid().validate().isEmpty());
    }

    // ── validation: required fields ──────────────────────────────────────────

    @Test
    void blankProfileName_producesError() {
        final MailProfile p = new MailProfile(null, "  ", "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Profile name is required.");
    }

    @Test
    void nullProfileName_producesError() {
        final MailProfile p = new MailProfile(null, null, "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Profile name is required.");
    }

    @Test
    void blankUsername_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "",
                "pop3s", "pop.gmail.com", 995, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Username is required.");
    }

    @Test
    void blankIncomingHost_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "", 995, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Incoming host is required.");
    }

    @Test
    void blankSmtpHost_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true, "", 587, true);
        assertContains(p.validate(), "SMTP host is required.");
    }

    // ── validation: port ranges ──────────────────────────────────────────────

    @Test
    void incomingPortZero_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 0, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Incoming port must be between 1 and 65535.");
    }

    @Test
    void incomingPortNegative_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", -1, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Incoming port must be between 1 and 65535.");
    }

    @Test
    void incomingPortTooBig_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 65536, true, "smtp.gmail.com", 587, true);
        assertContains(p.validate(), "Incoming port must be between 1 and 65535.");
    }

    @Test
    void smtpPortZero_producesError() {
        final MailProfile p = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true, "smtp.gmail.com", 0, true);
        assertContains(p.validate(), "SMTP port must be between 1 and 65535.");
    }

    @Test
    void validPortBoundaries_areAccepted() {
        final MailProfile low = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 1, true, "smtp.gmail.com", 1, true);
        final MailProfile high = new MailProfile(null, "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 65535, true, "smtp.gmail.com", 65535, true);
        assertTrue(low.validate().isEmpty());
        assertTrue(high.validate().isEmpty());
    }

    // ── id generation ────────────────────────────────────────────────────────

    @Test
    void nullId_isAutoAssigned() {
        final MailProfile p = valid();
        assertNotNull(p.getId());
        assertFalse(p.getId().isBlank());
    }

    @Test
    void explicitId_isPreserved() {
        final MailProfile p = new MailProfile("fixed-id", "Work", "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true, "smtp.gmail.com", 587, true);
        assertEquals("fixed-id", p.getId());
    }

    // ── getters ──────────────────────────────────────────────────────────────

    @Test
    void getters_returnConstructorValues() {
        final MailProfile p = new MailProfile("id1", "Home", "me@example.com",
                "imaps", "imap.example.com", 993, true, "smtp.example.com", 465, false);
        assertEquals("id1",              p.getId());
        assertEquals("Home",             p.getProfileName());
        assertEquals("me@example.com",   p.getUsername());
        assertEquals("imaps",            p.getIncomingProtocol());
        assertEquals("imap.example.com", p.getIncomingHost());
        assertEquals(993,                p.getIncomingPort());
        assertTrue(p.isIncomingSsl());
        assertEquals("smtp.example.com", p.getSmtpHost());
        assertEquals(465,                p.getSmtpPort());
        assertFalse(p.isSmtpStartTls());
        assertFalse(p.isTrustInvalidSsl());
    }

    @Test
    void trustInvalidSsl_canBeEnabled() {
        final MailProfile p = new MailProfile("id1", "Home", "me@example.com",
                "imaps", "imap.example.com", 993, true,
                "smtp.example.com", 587, true, true);

        assertTrue(p.isTrustInvalidSsl());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static void assertContains(List<String> errors, String expected) {
        assertTrue(errors.contains(expected),
                "Expected error '" + expected + "' in " + errors);
    }
}
