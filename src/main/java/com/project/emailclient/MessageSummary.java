package com.project.emailclient;

import javax.mail.Message;

/**
 * Immutable display model for a single inbox row.
 *
 * <p>Sender, subject, and date are pre-formatted strings for the Compose UI.
 * The original {@link Message} is retained as a package-private handle so
 * {@link MailService} can perform operations (content fetch, delete) without
 * exposing JavaMail types to the UI layer.
 */
public final class MessageSummary {

    public final String sender;
    public final String subject;
    public final String date;

    private final Message message;

    public MessageSummary(String sender, String subject, String date, Message message) {
        this.sender  = sender  != null ? sender  : "[none]";
        this.subject = subject != null ? subject : "[none]";
        this.date    = date    != null ? date    : "[none]";
        this.message = message;
    }

    /**
     * Package-private: only {@link MailService} should access the underlying message.
     * UI layers must not touch JavaMail objects directly.
     */
    Message getMessage() {
        return message;
    }
}
