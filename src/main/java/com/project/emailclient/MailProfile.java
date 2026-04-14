package com.project.emailclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Immutable value object representing a saved mail connection profile.
 * Holds all settings needed for incoming mail and SMTP, but never the password.
 */
public final class MailProfile {

    private final String id;
    private final String profileName;
    private final String username;
    private final String incomingProtocol; // pop3, pop3s, imap, imaps
    private final String incomingHost;
    private final int incomingPort;
    private final boolean incomingSsl;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean smtpStartTls;
    private final boolean trustInvalidSsl;

    /**
     * Creates a new profile. If {@code id} is null or blank a random UUID is assigned.
     */
    public MailProfile(String id, String profileName, String username,
                       String incomingProtocol, String incomingHost, int incomingPort, boolean incomingSsl,
                       String smtpHost, int smtpPort, boolean smtpStartTls) {
        this(id, profileName, username, incomingProtocol, incomingHost, incomingPort, incomingSsl,
                smtpHost, smtpPort, smtpStartTls, false);
    }

    /**
     * Creates a new profile. If {@code id} is null or blank a random UUID is assigned.
     */
    public MailProfile(String id, String profileName, String username,
                       String incomingProtocol, String incomingHost, int incomingPort, boolean incomingSsl,
                       String smtpHost, int smtpPort, boolean smtpStartTls, boolean trustInvalidSsl) {
        this.id               = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
        this.profileName      = profileName;
        this.username         = username;
        this.incomingProtocol = incomingProtocol;
        this.incomingHost     = incomingHost;
        this.incomingPort     = incomingPort;
        this.incomingSsl      = incomingSsl;
        this.smtpHost         = smtpHost;
        this.smtpPort         = smtpPort;
        this.smtpStartTls     = smtpStartTls;
        this.trustInvalidSsl  = trustInvalidSsl;
    }

    /**
     * Validates required fields and port ranges.
     *
     * @return an unmodifiable list of error messages; empty means valid
     */
    public List<String> validate() {
        final List<String> errors = new ArrayList<>();
        if (profileName == null || profileName.isBlank())
            errors.add("Profile name is required.");
        if (username == null || username.isBlank())
            errors.add("Username is required.");
        if (incomingProtocol == null || incomingProtocol.isBlank())
            errors.add("Incoming protocol is required.");
        if (incomingHost == null || incomingHost.isBlank())
            errors.add("Incoming host is required.");
        if (incomingPort < 1 || incomingPort > 65535)
            errors.add("Incoming port must be between 1 and 65535.");
        if (smtpHost == null || smtpHost.isBlank())
            errors.add("SMTP host is required.");
        if (smtpPort < 1 || smtpPort > 65535)
            errors.add("SMTP port must be between 1 and 65535.");
        return Collections.unmodifiableList(errors);
    }

    public String  getId()               { return id; }
    public String  getProfileName()      { return profileName; }
    public String  getUsername()         { return username; }
    public String  getIncomingProtocol() { return incomingProtocol; }
    public String  getIncomingHost()     { return incomingHost; }
    public int     getIncomingPort()     { return incomingPort; }
    public boolean isIncomingSsl()       { return incomingSsl; }
    public String  getSmtpHost()         { return smtpHost; }
    public int     getSmtpPort()         { return smtpPort; }
    public boolean isSmtpStartTls()      { return smtpStartTls; }
    public boolean isTrustInvalidSsl()   { return trustInvalidSsl; }

    /** Returns the profile name, used by JComboBox for display. */
    @Override
    public String toString() {
        return profileName + " <" + username + ">";
    }
}
