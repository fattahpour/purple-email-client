package com.project.emailclient;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.UUID;
import javax.swing.*;

/**
 * Modal dialog for creating or editing a {@link MailProfile}.
 * Call {@link #getResult()} after {@link #setVisible(true)} to retrieve the saved profile,
 * or {@code null} if the user cancelled.
 */
public class ProfileDialog extends JDialog {

    private static final String[] PROTOCOLS = {"pop3s", "pop3", "imaps", "imap"};

    private final JTextField        nameField         = new JTextField(25);
    private final JTextField        usernameField     = new JTextField(25);
    private final JComboBox<String> protocolBox       = new JComboBox<>(PROTOCOLS);
    private final JTextField        inHostField       = new JTextField(25);
    private final JTextField        inPortField       = new JTextField(6);
    private final JCheckBox         inSslCheck        = new JCheckBox("Use SSL/TLS", true);
    private final JTextField        smtpHostField     = new JTextField(25);
    private final JTextField        smtpPortField     = new JTextField(6);
    private final JCheckBox         smtpStartTlsCheck = new JCheckBox("Use STARTTLS", true);

    private final String existingId;
    private MailProfile result;

    /** Opens a dialog for creating a new profile. */
    public ProfileDialog(Frame parent) {
        this(parent, null);
    }

    /** Opens a dialog for editing an existing profile. */
    public ProfileDialog(Frame parent, MailProfile existing) {
        super(parent, existing == null ? "New Profile" : "Edit Profile", true);
        this.existingId = (existing != null) ? existing.getId() : UUID.randomUUID().toString();

        if (existing != null) {
            nameField.setText(existing.getProfileName());
            usernameField.setText(existing.getUsername());
            protocolBox.setSelectedItem(existing.getIncomingProtocol());
            inHostField.setText(existing.getIncomingHost());
            inPortField.setText(String.valueOf(existing.getIncomingPort()));
            inSslCheck.setSelected(existing.isIncomingSsl());
            smtpHostField.setText(existing.getSmtpHost());
            smtpPortField.setText(String.valueOf(existing.getSmtpPort()));
            smtpStartTlsCheck.setSelected(existing.isSmtpStartTls());
        } else {
            inPortField.setText("995");
            smtpPortField.setText("587");
        }

        // Auto-fill default port/SSL when protocol changes
        protocolBox.addActionListener(e -> autoFillPortFromProtocol());

        buildUI(parent);
    }

    private void autoFillPortFromProtocol() {
        final String proto = (String) protocolBox.getSelectedItem();
        if (proto == null) return;
        switch (proto) {
            case "pop3s" -> { inPortField.setText("995"); inSslCheck.setSelected(true); }
            case "pop3"  -> { inPortField.setText("110"); inSslCheck.setSelected(false); }
            case "imaps" -> { inPortField.setText("993"); inSslCheck.setSelected(true); }
            case "imap"  -> { inPortField.setText("143"); inSslCheck.setSelected(false); }
        }
    }

    private void buildUI(Frame parent) {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });

        // Main form
        final JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Profile Settings"));

        int row = 0;
        addFormRow(form, row++, "Profile Name:",     nameField);
        addFormRow(form, row++, "Username (email):", usernameField);
        addSeparatorRow(form, row++, "Incoming Mail");
        addFormRow(form, row++, "Protocol:",         protocolBox);
        addFormRow(form, row++, "Incoming Host:",    inHostField);
        addFormRow(form, row++, "Incoming Port:",    inPortField);
        addCheckRow(form, row++, inSslCheck);
        addSeparatorRow(form, row++, "Outgoing Mail (SMTP)");
        addFormRow(form, row++, "SMTP Host:",        smtpHostField);
        addFormRow(form, row++, "SMTP Port:",        smtpPortField);
        addCheckRow(form, row, smtpStartTlsCheck);

        // Bottom buttons
        final JPanel buttons = new JPanel();
        final JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> actionSave());
        final JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        buttons.add(saveBtn);
        buttons.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout(0, 4));
        getContentPane().add(form,    BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setLocationRelativeTo(parent);
    }

    private void addFormRow(JPanel panel, int row, String label, JComponent field) {
        final GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = row;
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(3, 5, 0, 2);
        panel.add(new JLabel(label), lc);

        final GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.gridy = row;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(3, 0, 0, 5);
        panel.add(field, fc);
    }

    private void addCheckRow(JPanel panel, int row, JCheckBox check) {
        final GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 1; cc.gridy = row;
        cc.anchor = GridBagConstraints.WEST;
        cc.insets = new Insets(3, 0, 0, 5);
        panel.add(check, cc);
    }

    private void addSeparatorRow(JPanel panel, int row, String text) {
        final JLabel label = new JLabel("--- " + text + " ---");
        label.setForeground(Color.GRAY);
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(8, 5, 2, 5);
        panel.add(label, c);
    }

    private void actionSave() {
        final int inPort;
        final int smtpPort;
        try {
            inPort = Integer.parseInt(inPortField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Incoming port must be a valid integer.");
            return;
        }
        try {
            smtpPort = Integer.parseInt(smtpPortField.getText().trim());
        } catch (NumberFormatException e) {
            showError("SMTP port must be a valid integer.");
            return;
        }

        final MailProfile candidate = new MailProfile(
                existingId,
                nameField.getText().trim(),
                usernameField.getText().trim(),
                (String) protocolBox.getSelectedItem(),
                inHostField.getText().trim(),
                inPort,
                inSslCheck.isSelected(),
                smtpHostField.getText().trim(),
                smtpPort,
                smtpStartTlsCheck.isSelected()
        );

        final List<String> errors = candidate.validate();
        if (!errors.isEmpty()) {
            showError(String.join("\n", errors));
            return;
        }

        result = candidate;
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Returns the profile the user saved, or {@code null} if the dialog was cancelled.
     * Only valid after the dialog has been shown and closed.
     */
    public MailProfile getResult() {
        return result;
    }
}
