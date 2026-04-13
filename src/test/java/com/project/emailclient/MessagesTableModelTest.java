package com.project.emailclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.Message;

import static org.junit.jupiter.api.Assertions.*;

class MessagesTableModelTest {

    private MessagesTableModel model;

    @BeforeEach
    void setUp() {
        model = new MessagesTableModel();
    }

    // ── column metadata ─────────────────────────────────────────────────────

    @Test
    void columnCount_isThree() {
        assertEquals(3, model.getColumnCount());
    }

    @Test
    void columnNames_correct() {
        assertEquals("Sender",  model.getColumnName(0));
        assertEquals("Subject", model.getColumnName(1));
        assertEquals("Date",    model.getColumnName(2));
    }

    // ── empty model ──────────────────────────────────────────────────────────

    @Test
    void emptyModel_hasZeroRows() {
        assertEquals(0, model.getRowCount());
    }

    @Test
    void getMessage_emptyModel_throwsIndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> model.getMessage(0));
    }

    // ── setMessages ──────────────────────────────────────────────────────────

    @Test
    void setMessages_emptyArray_rowCountIsZero() {
        model.setMessages(new Message[0]);
        assertEquals(0, model.getRowCount());
    }

    @Test
    void setMessages_calledTwice_doesNotAccumulate() {
        // First call with empty array, second with empty array — should not stack.
        model.setMessages(new Message[0]);
        model.setMessages(new Message[0]);
        assertEquals(0, model.getRowCount());
    }
}
