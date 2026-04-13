package com.project.emailclient;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Modal dialog that lets the user select a saved {@link MailProfile} and enter a password.
 * Profile management (add, edit, delete) is also available from this dialog.
 *
 * <p>After {@link #setVisible(true)} returns, call {@link #getSelectedProfile()} and
 * {@link #getPassword()}. Both return {@code null} when the user cancels.
 */
public class ConnectDialog extends JDialog {

    private final MailProfileStore store;
    private final DefaultComboBoxModel<MailProfile> profileModel = new DefaultComboBoxModel<>();
    private final JComboBox<MailProfile> profileComboBox = new JComboBox<>(profileModel);
    private final JPasswordField passwordField = new JPasswordField();

    // Set only when the user clicks Connect successfully.
    private MailProfile selectedProfile;
    private String      enteredPassword;

    public ConnectDialog(Frame parent, MailProfileStore store) {
        super(parent, "Connect", true);
        this.store = store;

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });

        refreshProfileList();

        // Settings panel
        final JPanel settings = new JPanel(new GridBagLayout());
        settings.setBorder(BorderFactory.createTitledBorder("Connection Settings"));
        addRow(settings, 0, "Profile:",  profileComboBox);
        addRow(settings, 1, "Password:", passwordField);

        // Profile action buttons
        final JPanel profileActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        profileActions.setBorder(BorderFactory.createTitledBorder("Profiles"));
        final JButton addBtn = new JButton("New Profile");
        addBtn.addActionListener(e -> actionAddProfile());
        final JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> actionEditProfile());
        final JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> actionDeleteProfile());
        profileActions.add(addBtn);
        profileActions.add(editBtn);
        profileActions.add(deleteBtn);

        // Connect / Cancel buttons
        final JPanel buttons = new JPanel();
        final JButton connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> actionConnect());
        final JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        buttons.add(connectBtn);
        buttons.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout(0, 4));
        getContentPane().add(settings,       BorderLayout.NORTH);
        getContentPane().add(profileActions, BorderLayout.CENTER);
        getContentPane().add(buttons,        BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(400, getHeight()));
        setLocationRelativeTo(parent);
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        final GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = row;
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(5, 5, 5, 2);
        panel.add(new JLabel(label), lc);

        final GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.gridy = row;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(5, 0, 5, 5);
        panel.add(field, fc);
    }

    private void refreshProfileList() {
        final MailProfile currentSelection = (MailProfile) profileComboBox.getSelectedItem();
        profileModel.removeAllElements();

        for (final MailProfile p : store.getProfiles()) {
            profileModel.addElement(p);
        }

        // Restore previous selection, or fall back to last-used, or first entry
        if (currentSelection != null) {
            for (int i = 0; i < profileModel.getSize(); i++) {
                if (profileModel.getElementAt(i).getId().equals(currentSelection.getId())) {
                    profileComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        final String lastId = store.getLastUsedProfileId();
        if (lastId != null) {
            for (int i = 0; i < profileModel.getSize(); i++) {
                if (profileModel.getElementAt(i).getId().equals(lastId)) {
                    profileComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (profileModel.getSize() > 0) {
            profileComboBox.setSelectedIndex(0);
        }
    }

    private void actionAddProfile() {
        final ProfileDialog dlg = new ProfileDialog((Frame) getOwner());
        dlg.setVisible(true);
        final MailProfile p = dlg.getResult();
        if (p != null) {
            store.addProfile(p);
            refreshProfileList();
            profileComboBox.setSelectedItem(p);
        }
    }

    private void actionEditProfile() {
        final MailProfile selected = (MailProfile) profileComboBox.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No profile selected.", "Edit Profile",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        final ProfileDialog dlg = new ProfileDialog((Frame) getOwner(), selected);
        dlg.setVisible(true);
        final MailProfile updated = dlg.getResult();
        if (updated != null) {
            store.updateProfile(updated);
            refreshProfileList();
            // Re-select the updated profile
            for (int i = 0; i < profileModel.getSize(); i++) {
                if (profileModel.getElementAt(i).getId().equals(updated.getId())) {
                    profileComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void actionDeleteProfile() {
        final MailProfile selected = (MailProfile) profileComboBox.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No profile selected.", "Delete Profile",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        final int confirm = JOptionPane.showConfirmDialog(this,
                "Delete profile \"" + selected.getProfileName() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            store.deleteProfile(selected.getId());
            refreshProfileList();
        }
    }

    private void actionConnect() {
        final MailProfile profile = (MailProfile) profileComboBox.getSelectedItem();
        if (profile == null) {
            JOptionPane.showMessageDialog(this,
                    "No profile selected. Please create a profile first.",
                    "No Profile", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (passwordField.getPassword().length < 1) {
            JOptionPane.showMessageDialog(this, "Password is required.",
                    "Missing Password", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedProfile = profile;
        enteredPassword = new String(passwordField.getPassword());
        dispose();
    }

    /**
     * Returns the profile the user chose to connect with.
     * {@code null} means the dialog was cancelled.
     */
    public MailProfile getSelectedProfile() {
        return selectedProfile;
    }

    /**
     * Returns the password entered at connection time. Never persisted.
     * {@code null} when the dialog was cancelled.
     */
    public String getPassword() {
        return enteredPassword;
    }
}
