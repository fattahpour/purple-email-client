package com.project.emailclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and saves {@link MailProfile} objects to a properties file on disk.
 *
 * <p>Default location: {@code ${user.home}/.java-email-client/profiles.properties}.
 * Pass a custom {@link Path} to the single-argument constructor for testing.
 *
 * <p>Properties format (N = 0-based index):
 * <pre>
 *   lastUsedProfileId=&lt;id&gt;
 *   profile.count=N
 *   profile.N.id=...
 *   profile.N.name=...
 *   profile.N.username=...
 *   profile.N.incomingProtocol=pop3s|pop3|imaps|imap
 *   profile.N.incomingHost=...
 *   profile.N.incomingPort=...
 *   profile.N.incomingSsl=true|false
 *   profile.N.smtpHost=...
 *   profile.N.smtpPort=...
 *   profile.N.smtpStartTls=true|false
 *   profile.N.trustInvalidSsl=true|false
 * </pre>
 */
public class MailProfileStore {

    private static final Logger LOG = Logger.getLogger(MailProfileStore.class.getName());

    private static final Path DEFAULT_CONFIG_FILE =
            Paths.get(System.getProperty("user.home"), ".java-email-client", "profiles.properties");

    private final Path configFile;
    private final List<MailProfile> profiles = new ArrayList<>();
    private String lastUsedProfileId;

    /** Default constructor — persists to {@code ${user.home}/.java-email-client/profiles.properties}. */
    public MailProfileStore() {
        this(DEFAULT_CONFIG_FILE);
    }

    /** Constructor for tests or custom locations. Pass a temporary file path. */
    public MailProfileStore(Path configFile) {
        this.configFile = configFile;
    }

    // ── load ─────────────────────────────────────────────────────────────────

    /**
     * Loads all profiles from disk. Clears any previously loaded state.
     * Missing files are silently ignored. Corrupt individual profiles are
     * logged and skipped; they do not prevent other profiles from loading.
     */
    public void load() {
        profiles.clear();
        lastUsedProfileId = null;

        if (!Files.exists(configFile)) {
            return;
        }

        final Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read profiles config: " + configFile, e);
            return;
        }

        lastUsedProfileId = props.getProperty("lastUsedProfileId");

        final int count = parseIntSafe(props.getProperty("profile.count", "0"), 0);
        for (int i = 0; i < count; i++) {
            try {
                final MailProfile p = readProfile(props, i);
                if (p != null) {
                    profiles.add(p);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Skipping invalid profile at index " + i, e);
            }
        }
    }

    private MailProfile readProfile(Properties props, int idx) {
        final String prefix = "profile." + idx + ".";
        final String id       = props.getProperty(prefix + "id", "").trim();
        final String name     = props.getProperty(prefix + "name", "").trim();
        final String username = props.getProperty(prefix + "username", "").trim();
        final String protocol = props.getProperty(prefix + "incomingProtocol", "pop3s").trim();
        final String inHost   = props.getProperty(prefix + "incomingHost", "").trim();
        final int    inPort   = parseIntSafe(props.getProperty(prefix + "incomingPort", "995"), 995);
        final boolean inSsl   = Boolean.parseBoolean(props.getProperty(prefix + "incomingSsl", "true"));
        final String smtpHost = props.getProperty(prefix + "smtpHost", "").trim();
        final int    smtpPort = parseIntSafe(props.getProperty(prefix + "smtpPort", "587"), 587);
        final boolean startTls = Boolean.parseBoolean(props.getProperty(prefix + "smtpStartTls", "true"));
        final boolean trustInvalidSsl =
                Boolean.parseBoolean(props.getProperty(prefix + "trustInvalidSsl", "false"));

        if (id.isBlank() || name.isBlank()) {
            return null;
        }

        final MailProfile p = new MailProfile(id, name, username, protocol,
                inHost, inPort, inSsl, smtpHost, smtpPort, startTls, trustInvalidSsl);

        final List<String> errors = p.validate();
        if (!errors.isEmpty()) {
            LOG.warning("Skipping invalid profile '" + name + "': " + errors);
            return null;
        }
        return p;
    }

    // ── save ─────────────────────────────────────────────────────────────────

    /**
     * Saves all profiles to disk. Creates the config directory if needed.
     * Logs but does not throw on I/O errors.
     */
    public void save() {
        try {
            final Path dir = configFile.getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }

            final Properties props = new Properties();
            if (lastUsedProfileId != null) {
                props.setProperty("lastUsedProfileId", lastUsedProfileId);
            }
            props.setProperty("profile.count", String.valueOf(profiles.size()));
            for (int i = 0; i < profiles.size(); i++) {
                writeProfile(props, i, profiles.get(i));
            }

            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "Java Email Client - mail profiles");
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not save profiles config: " + configFile, e);
        }
    }

    private void writeProfile(Properties props, int idx, MailProfile p) {
        final String prefix = "profile." + idx + ".";
        props.setProperty(prefix + "id",               p.getId());
        props.setProperty(prefix + "name",             p.getProfileName());
        props.setProperty(prefix + "username",         p.getUsername());
        props.setProperty(prefix + "incomingProtocol", p.getIncomingProtocol());
        props.setProperty(prefix + "incomingHost",     p.getIncomingHost());
        props.setProperty(prefix + "incomingPort",     String.valueOf(p.getIncomingPort()));
        props.setProperty(prefix + "incomingSsl",      String.valueOf(p.isIncomingSsl()));
        props.setProperty(prefix + "smtpHost",         p.getSmtpHost());
        props.setProperty(prefix + "smtpPort",         String.valueOf(p.getSmtpPort()));
        props.setProperty(prefix + "smtpStartTls",     String.valueOf(p.isSmtpStartTls()));
        props.setProperty(prefix + "trustInvalidSsl",  String.valueOf(p.isTrustInvalidSsl()));
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /** Returns an unmodifiable view of all loaded profiles. */
    public List<MailProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    /** Adds a profile and immediately saves. */
    public void addProfile(MailProfile profile) {
        profiles.add(profile);
        save();
    }

    /**
     * Replaces the profile with the same ID as {@code updated} and saves.
     * If no matching profile is found this is a no-op.
     */
    public void updateProfile(MailProfile updated) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(updated.getId())) {
                profiles.set(i, updated);
                save();
                return;
            }
        }
    }

    /**
     * Removes the profile with the given ID and saves.
     * Also clears {@code lastUsedProfileId} if it matches.
     */
    public void deleteProfile(String profileId) {
        profiles.removeIf(p -> p.getId().equals(profileId));
        if (profileId != null && profileId.equals(lastUsedProfileId)) {
            lastUsedProfileId = null;
        }
        save();
    }

    /** Returns the profile with the given ID, or {@code null} if not found. */
    public MailProfile findById(String profileId) {
        if (profileId == null) return null;
        return profiles.stream()
                .filter(p -> p.getId().equals(profileId))
                .findFirst()
                .orElse(null);
    }

    public String getLastUsedProfileId() {
        return lastUsedProfileId;
    }

    /** Persists the last-used profile ID immediately. */
    public void setLastUsedProfileId(String id) {
        this.lastUsedProfileId = id;
        save();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static int parseIntSafe(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
