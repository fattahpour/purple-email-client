package com.project.emailclient;

/**
 * Per-provider server settings for Gmail, Yahoo, and Hotmail/Outlook.
 *
 * <p>The SMTP host strings match the values shown in {@link ConnectDialog}'s
 * provider combo box, so callers can look up settings by the selected SMTP host.
 */
public enum ProviderConfig {

    GMAIL("gmail", "pop.gmail.com", "smtp.gmail.com", 995, 587, "pop3s"),
    YAHOO("yahoo", "pop.mail.yahoo.com", "smtp.mail.yahoo.com", 995, 587, "pop3s"),
    HOTMAIL("hotmail", "pop3.live.com", "smtp.live.com", 995, 587, "pop3s");

    private final String name;
    private final String incomingHost;
    private final String smtpHost;
    private final int incomingPort;
    private final int smtpPort;
    private final String storeProtocol;

    ProviderConfig(String name, String incomingHost, String smtpHost,
                   int incomingPort, int smtpPort, String storeProtocol) {
        this.name = name;
        this.incomingHost = incomingHost;
        this.smtpHost = smtpHost;
        this.incomingPort = incomingPort;
        this.smtpPort = smtpPort;
        this.storeProtocol = storeProtocol;
    }

    public String getName()         { return name; }
    public String getIncomingHost() { return incomingHost; }
    public String getSmtpHost()     { return smtpHost; }
    public int    getIncomingPort() { return incomingPort; }
    public int    getSmtpPort()     { return smtpPort; }
    public String getStoreProtocol(){ return storeProtocol; }

    /**
     * Find a provider by its SMTP host string (as returned by
     * {@link ConnectDialog#getSmtpServer()}).
     *
     * @return the matching config, or {@code null} if unknown
     */
    public static ProviderConfig forSmtpHost(String smtpHost) {
        if (smtpHost == null) return null;
        for (final ProviderConfig p : values()) {
            if (p.smtpHost.equalsIgnoreCase(smtpHost)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Find a provider by its short name (e.g. "gmail", "yahoo", "hotmail").
     *
     * @return the matching config, or {@code null} if unknown
     */
    public static ProviderConfig forName(String name) {
        if (name == null) return null;
        for (final ProviderConfig p : values()) {
            if (p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}
