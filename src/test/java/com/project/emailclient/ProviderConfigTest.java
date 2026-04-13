package com.project.emailclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderConfigTest {

    @Test
    void allProvidersHaveRequiredFields() {
        for (final ProviderConfig p : ProviderConfig.values()) {
            assertNotNull(p.getName(),          p + " name should not be null");
            assertNotNull(p.getSmtpHost(),      p + " SMTP host should not be null");
            assertNotNull(p.getIncomingHost(),  p + " incoming host should not be null");
            assertNotNull(p.getStoreProtocol(), p + " store protocol should not be null");
            assertTrue(p.getSmtpPort()     > 0, p + " SMTP port should be positive");
            assertTrue(p.getIncomingPort() > 0, p + " incoming port should be positive");
        }
    }

    @Test
    void lookupBySmtpHost_gmail() {
        final ProviderConfig p = ProviderConfig.forSmtpHost("smtp.gmail.com");
        assertNotNull(p, "Gmail provider should be found");
        assertEquals("gmail", p.getName());
        assertEquals("pop.gmail.com", p.getIncomingHost());
        assertEquals("smtp.gmail.com", p.getSmtpHost());
        assertEquals(995, p.getIncomingPort());
        assertEquals(587, p.getSmtpPort());
    }

    @Test
    void lookupBySmtpHost_yahoo() {
        final ProviderConfig p = ProviderConfig.forSmtpHost("smtp.mail.yahoo.com");
        assertNotNull(p);
        assertEquals("yahoo", p.getName());
        assertEquals("pop.mail.yahoo.com", p.getIncomingHost());
    }

    @Test
    void lookupBySmtpHost_hotmail() {
        final ProviderConfig p = ProviderConfig.forSmtpHost("smtp.live.com");
        assertNotNull(p);
        assertEquals("hotmail", p.getName());
        assertEquals("pop3.live.com", p.getIncomingHost());
    }

    @Test
    void lookupBySmtpHost_caseInsensitive() {
        assertNotNull(ProviderConfig.forSmtpHost("SMTP.GMAIL.COM"));
        assertNotNull(ProviderConfig.forSmtpHost("Smtp.Mail.Yahoo.Com"));
    }

    @Test
    void lookupBySmtpHost_unknownReturnsNull() {
        assertNull(ProviderConfig.forSmtpHost("smtp.unknown.example.com"));
        assertNull(ProviderConfig.forSmtpHost(null));
    }

    @Test
    void lookupByName_gmail() {
        final ProviderConfig p = ProviderConfig.forName("gmail");
        assertNotNull(p);
        assertEquals(ProviderConfig.GMAIL, p);
    }

    @Test
    void lookupByName_caseInsensitive() {
        assertNotNull(ProviderConfig.forName("GMAIL"));
        assertNotNull(ProviderConfig.forName("Yahoo"));
        assertNotNull(ProviderConfig.forName("HOTMAIL"));
    }

    @Test
    void lookupByName_unknownReturnsNull() {
        assertNull(ProviderConfig.forName("aol"));
        assertNull(ProviderConfig.forName(null));
    }
}
