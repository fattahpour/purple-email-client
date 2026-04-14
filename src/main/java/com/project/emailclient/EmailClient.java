package com.project.emailclient;

import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;

/** The E-mail Client main window. */
class EmailClient extends JFrame {

    private static final Logger LOG = Logger.getLogger(EmailClient.class.getName());

    // Message table's data model.
    private MessagesTableModel tableModel;

    // Table listing messages.
    private JTable table;

    // Text area for displaying the selected message's content.
    private JTextArea messageTextArea;

    // Split panel that holds the messages table and the message view panel.
    private JSplitPane splitPane;

    // Buttons for managing the selected message.
    private JButton replyButton, forwardButton, deleteButton;

    // Currently selected message in table.
    private Message selectedMessage;

    // Flag for whether a message is being deleted.
    private boolean deleting;

    // Active connection state — populated after a successful connect.
    private MailProfile currentProfile;
    private String      currentPassword;

    // Profile store — loaded once on startup, shared with ConnectDialog.
    private final MailProfileStore profileStore = new MailProfileStore();

    // Constructor for E-mail Client.
    public EmailClient() {
        profileStore.load();

        setTitle("E-mail Client");
        setSize(640, 480);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // File menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(e -> actionExit());
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Top button panel
        JPanel buttonPanel = new JPanel();
        JButton newButton = new JButton("Compose Mail");
        newButton.addActionListener(e -> {
            try {
                actionNew();
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
        buttonPanel.add(newButton);

        // Messages table
        tableModel = new MessagesTableModel();
        table = new JTable((TableModel) tableModel);
        table.getSelectionModel().addListSelectionListener(e -> tableSelectionChanged());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // E-mails panel
        JPanel emailsPanel = new JPanel();
        emailsPanel.setBorder(BorderFactory.createTitledBorder("E-mails"));
        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(messageTextArea));
        emailsPanel.setLayout(new BorderLayout());
        emailsPanel.add(splitPane, BorderLayout.CENTER);

        // Bottom button panel
        JPanel buttonPanel2 = new JPanel();
        replyButton = new JButton("Reply");
        replyButton.addActionListener(e -> {
            try {
                actionReply();
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
        replyButton.setEnabled(false);
        buttonPanel2.add(replyButton);

        forwardButton = new JButton("Forward");
        forwardButton.addActionListener(e -> {
            try {
                actionForward();
            } catch (MessagingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        });
        forwardButton.setEnabled(false);
        buttonPanel2.add(forwardButton);

        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> actionDelete());
        deleteButton.setEnabled(false);
        buttonPanel2.add(deleteButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel,  BorderLayout.NORTH);
        getContentPane().add(emailsPanel,  BorderLayout.CENTER);
        getContentPane().add(buttonPanel2, BorderLayout.SOUTH);
    }

    private void actionExit() {
        System.exit(0);
    }

    private void actionNew() throws MessagingException {
        sendMessage(MessageDialog.NEW, null);
    }

    private void tableSelectionChanged() {
        if (!deleting) {
            selectedMessage = tableModel.getMessage(table.getSelectedRow());
            showSelectedMessage();
            updateButtons();
        }
    }

    private void actionReply() throws MessagingException {
        sendMessage(MessageDialog.REPLY, selectedMessage);
    }

    private void actionForward() throws MessagingException {
        sendMessage(MessageDialog.FORWARD, selectedMessage);
    }

    private void actionDelete() {
        deleting = true;
        try {
            selectedMessage.setFlag(Flags.Flag.DELETED, true);
            final Folder folder = selectedMessage.getFolder();
            folder.close(true);
            folder.open(Folder.READ_WRITE);
        } catch (Exception e) {
            showError("Unable to delete message.", false);
        }
        tableModel.deleteMessage(table.getSelectedRow());
        messageTextArea.setText("");
        deleting = false;
        selectedMessage = null;
        updateButtons();
    }

    // Send the specified message using the active profile's SMTP settings.
    private void sendMessage(int type, Message message) throws MessagingException {
        if (currentProfile == null) {
            showError("Not connected. Please connect first.", false);
            return;
        }

        final MessageDialog dialog;
        try {
            final String defaultFrom = currentProfile.getUsername();
            dialog = new MessageDialog(this, type, message, defaultFrom);
            if (!dialog.display()) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to send message.", false);
            return;
        }

        final Properties props = new Properties();
        props.put("mail.smtp.host", currentProfile.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(currentProfile.getSmtpPort()));
        props.put("mail.smtp.auth", "true");
        if (currentProfile.isSmtpStartTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            if (currentProfile.isTrustInvalidSsl()) {
                // Opt-in for self-signed or otherwise invalid certificates.
                props.put("mail.smtp.ssl.trust", "*");
            }
        }

        final String smtpUser = currentProfile.getUsername();
        final String smtpPass = currentPassword;
        final Session smtpSession = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUser, smtpPass);
                    }
                });

        try {
            final MimeMessage newMessage = new MimeMessage(smtpSession);
            newMessage.setFrom(new InternetAddress(dialog.getFrom()));
            newMessage.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(dialog.getTo()));
            newMessage.setSubject(dialog.getSubject());
            newMessage.setSentDate(new Date());
            newMessage.setText(dialog.getContent());

            try (Transport transport = smtpSession.getTransport("smtp")) {
                transport.connect(currentProfile.getSmtpHost(), smtpUser, smtpPass);
                transport.sendMessage(newMessage, newMessage.getAllRecipients());
            }
            showSuccess("Message Sent Successfully.");
        } catch (MessagingException mex) {
            LOG.log(Level.SEVERE, "Unable to send message", mex);
            showError("Unable to send message.", false);
        }
    }

    private void showSelectedMessage() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            messageTextArea.setText(getMessageContent(selectedMessage));
            messageTextArea.setCaretPosition(0);
        } catch (Exception e) {
            showError("Unable to load message.", false);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void updateButtons() {
        final boolean hasMessage = selectedMessage != null;
        replyButton.setEnabled(hasMessage);
        forwardButton.setEnabled(hasMessage);
        deleteButton.setEnabled(hasMessage);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            splitPane.setDividerLocation(.5);
        }
    }

    // Connect to e-mail server using a saved profile.
    public void connect() {
        final ConnectDialog dialog = new ConnectDialog(this, profileStore);
        dialog.setVisible(true);

        final MailProfile profile = dialog.getSelectedProfile();
        final String password    = dialog.getPassword();

        if (profile == null || password == null) {
            // User cancelled — do nothing.
            return;
        }

        currentProfile  = profile;
        currentPassword = password;

        // Persist last-used selection.
        profileStore.setLastUsedProfileId(profile.getId());

        // Show a "Downloading…" modal while fetching on a background thread.
        final DownloadingDialog downloadingDialog = new DownloadingDialog(this);

        final SwingWorker<Message[], Void> worker = new SwingWorker<>() {
            @Override
            protected Message[] doInBackground() throws Exception {
                final String proto  = profile.getIncomingProtocol();
                final String prefix = incomingPropPrefix(proto);

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

                final Session emailSession = Session.getInstance(props);
                final Store store = emailSession.getStore(proto);
                store.connect(profile.getIncomingHost(), profile.getIncomingPort(),
                        profile.getUsername(), password);
                final Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_WRITE);
                final Message[] messages = folder.getMessages();
                final FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.ENVELOPE);
                folder.fetch(messages, fp);
                return messages;
            }

            @Override
            protected void done() {
                downloadingDialog.dispose();
                try {
                    tableModel.setMessages(get());
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Unable to download messages", e);
                    showError("Unable to download messages.", false);
                }
            }
        };

        worker.execute();
        // Modal dialog blocks this thread (pumping events) until done() disposes it.
        downloadingDialog.setVisible(true);
    }

    /**
     * Returns the JavaMail property prefix for an incoming-mail protocol.
     * SSL protocols have their own property namespace.
     */
    private static String incomingPropPrefix(String protocol) {
        if (protocol == null) return "pop3";
        if (protocol.startsWith("imaps")) return "imaps";
        if (protocol.startsWith("imap")) return "imap";
        if (protocol.startsWith("pop3s")) return "pop3s";
        return "pop3";
    }

    private void showError(String message, boolean exit) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        if (exit) System.exit(0);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Get a message's content, preferring plain text over HTML. */
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

    public static void main(String[] args) {
        final EmailClient client = new EmailClient();
        client.setVisible(true);
        client.connect();
    }
}
