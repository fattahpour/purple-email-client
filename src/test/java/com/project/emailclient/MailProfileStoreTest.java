package com.project.emailclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailProfileStoreTest {

    @TempDir
    Path tempDir;

    private Path configFile;
    private MailProfileStore store;

    @BeforeEach
    void setUp() {
        configFile = tempDir.resolve("profiles.properties");
        store = new MailProfileStore(configFile);
    }

    // ── load with no file ────────────────────────────────────────────────────

    @Test
    void load_missingFile_returnsEmptyList() {
        store.load();
        assertTrue(store.getProfiles().isEmpty());
        assertNull(store.getLastUsedProfileId());
    }

    // ── round-trip: single profile ───────────────────────────────────────────

    @Test
    void saveAndLoad_singleProfile_roundTrips() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();

        assertEquals(1, reloaded.getProfiles().size());
        final MailProfile p = reloaded.getProfiles().get(0);
        assertEquals("id1",            p.getId());
        assertEquals("Gmail Work",     p.getProfileName());
        assertEquals("user@gmail.com", p.getUsername());
        assertEquals("pop3s",          p.getIncomingProtocol());
        assertEquals("pop.gmail.com",  p.getIncomingHost());
        assertEquals(995,              p.getIncomingPort());
        assertTrue(p.isIncomingSsl());
        assertEquals("smtp.gmail.com", p.getSmtpHost());
        assertEquals(587,              p.getSmtpPort());
        assertTrue(p.isSmtpStartTls());
    }

    // ── round-trip: multiple profiles ────────────────────────────────────────

    @Test
    void saveAndLoad_multipleProfiles_allPreserved() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        store.addProfile(yahooProfile("id2", "Yahoo Personal"));

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();

        assertEquals(2, reloaded.getProfiles().size());
        assertEquals("Gmail Work",     reloaded.getProfiles().get(0).getProfileName());
        assertEquals("Yahoo Personal", reloaded.getProfiles().get(1).getProfileName());
    }

    // ── last-used profile ────────────────────────────────────────────────────

    @Test
    void setLastUsedProfileId_persistsAndReloads() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        store.setLastUsedProfileId("id1");

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();

        assertEquals("id1", reloaded.getLastUsedProfileId());
    }

    @Test
    void lastUsedProfileId_isNullWhenNeverSet() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        // Do not call setLastUsedProfileId

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();

        assertNull(reloaded.getLastUsedProfileId());
    }

    // ── updateProfile ────────────────────────────────────────────────────────

    @Test
    void updateProfile_replacesExistingEntry() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));

        final MailProfile updated = new MailProfile("id1", "Gmail Work (Updated)",
                "updated@gmail.com", "imaps", "imap.gmail.com", 993, true,
                "smtp.gmail.com", 587, true);
        store.updateProfile(updated);

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();
        assertEquals(1, reloaded.getProfiles().size());
        assertEquals("Gmail Work (Updated)", reloaded.getProfiles().get(0).getProfileName());
        assertEquals("updated@gmail.com",    reloaded.getProfiles().get(0).getUsername());
    }

    // ── deleteProfile ────────────────────────────────────────────────────────

    @Test
    void deleteProfile_removesItFromList() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        store.addProfile(yahooProfile("id2", "Yahoo Personal"));

        store.deleteProfile("id1");

        assertEquals(1, store.getProfiles().size());
        assertEquals("id2", store.getProfiles().get(0).getId());
    }

    @Test
    void deleteProfile_persistsAfterReload() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        store.addProfile(yahooProfile("id2", "Yahoo Personal"));
        store.deleteProfile("id1");

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();

        assertEquals(1, reloaded.getProfiles().size());
        assertEquals("id2", reloaded.getProfiles().get(0).getId());
    }

    @Test
    void deleteProfile_clearsLastUsedIfItMatches() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        store.setLastUsedProfileId("id1");
        store.deleteProfile("id1");

        assertNull(store.getLastUsedProfileId());

        final MailProfileStore reloaded = new MailProfileStore(configFile);
        reloaded.load();
        assertNull(reloaded.getLastUsedProfileId());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_returnsMatchingProfile() {
        store.load();
        store.addProfile(gmailProfile("id1", "Gmail Work"));
        store.addProfile(yahooProfile("id2", "Yahoo Personal"));

        final MailProfile found = store.findById("id2");
        assertNotNull(found);
        assertEquals("Yahoo Personal", found.getProfileName());
    }

    @Test
    void findById_returnsNullForUnknownId() {
        store.load();
        assertNull(store.findById("nonexistent"));
        assertNull(store.findById(null));
    }

    // ── getProfiles is unmodifiable ──────────────────────────────────────────

    @Test
    void getProfiles_returnsUnmodifiableList() {
        store.load();
        final List<MailProfile> profiles = store.getProfiles();
        assertThrows(UnsupportedOperationException.class,
                () -> profiles.add(gmailProfile("x", "X")));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static MailProfile gmailProfile(String id, String name) {
        return new MailProfile(id, name, "user@gmail.com",
                "pop3s", "pop.gmail.com", 995, true,
                "smtp.gmail.com", 587, true);
    }

    private static MailProfile yahooProfile(String id, String name) {
        return new MailProfile(id, name, "user@yahoo.com",
                "pop3s", "pop.mail.yahoo.com", 995, true,
                "smtp.mail.yahoo.com", 587, true);
    }
}
