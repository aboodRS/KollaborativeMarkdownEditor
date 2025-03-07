package com.github.aboodRS.collaborative_markdown_editor_server;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MarkdownEditor extends JPanel {
    private static final long serialVersionUID = 1L;

    // Text pane for the markdown content
    private JTextPane markdownPane;
    
    // Timer to control update delay for styles
    private Timer debounceTimer; 
    
    // Styles for different markdown elements
    private final Style header1Style;
    private final Style header2Style;
    private final Style boldStyle;
    private final Style italicStyle;
    private final Style codeBlockStyle;
    private final Style blockquoteStyle;
    private final Style listItemStyle;
    private final Style tableStyle;
    private final Style tableHeaderStyle;
    
    // List of listeners to handle text changes
    private final List<TextChangeListener> textChangeListeners = new ArrayList<>(); // List of listeners to handle text changes

    public MarkdownEditor() {
        setLayout(new BorderLayout());
        
        // Initialize markdownPane with basic styling and content settings
        markdownPane = new JTextPane();
        markdownPane.setMargin(new Insets(10, 10, 10, 10));
        markdownPane.setContentType("text/plain");
        markdownPane.setBackground(new Color(40, 44, 52));
        markdownPane.setForeground(Color.WHITE);
        markdownPane.setFont(new Font("SansSerif", 0, 14));
        markdownPane.setCaretColor(Color.WHITE);
        
        // Add markdownPane to a scroll pane and add it to the main panel
        JScrollPane scrollPane = new JScrollPane(this.markdownPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.add(scrollPane, BorderLayout.CENTER);

        // Initialize document and add a DocumentListener to detect text changes
        StyledDocument doc = markdownPane.getStyledDocument();
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceUpdateStyles();
                notifyTextChangeListeners();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceUpdateStyles();
                notifyTextChangeListeners();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceUpdateStyles();
                notifyTextChangeListeners();
            }
        });

        // Define styles for various markdown elements
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle("default");
        this.header1Style = doc.addStyle("header1", defaultStyle);
        StyleConstants.setFontSize(this.header1Style, 24);
        StyleConstants.setBold(this.header1Style, true);

        this.header2Style = doc.addStyle("header2", defaultStyle);
        StyleConstants.setFontSize(this.header2Style, 18);
        StyleConstants.setBold(this.header2Style, true);

        this.boldStyle = doc.addStyle("bold", defaultStyle);
        StyleConstants.setBold(this.boldStyle, true);

        this.italicStyle = doc.addStyle("italic", defaultStyle);
        StyleConstants.setItalic(this.italicStyle, true);

        this.codeBlockStyle = doc.addStyle("code", defaultStyle);
        StyleConstants.setFontFamily(this.codeBlockStyle, "Monospaced");
        StyleConstants.setBackground(this.codeBlockStyle, new Color(230, 230, 230));

        this.blockquoteStyle = doc.addStyle("blockquote", defaultStyle);
        StyleConstants.setItalic(this.blockquoteStyle, true);
        StyleConstants.setForeground(this.blockquoteStyle, Color.GRAY);

        this.listItemStyle = doc.addStyle("listItem", defaultStyle);
        StyleConstants.setFontSize(this.listItemStyle, 14);

        this.tableStyle = doc.addStyle("table", defaultStyle);
        StyleConstants.setFontSize(this.tableStyle, 14);

        this.tableHeaderStyle = doc.addStyle("tableHeader", defaultStyle);
        StyleConstants.setBold(this.tableHeaderStyle, true);
        StyleConstants.setFontSize(this.tableHeaderStyle, 14);
    }

    // Debounces style application by delaying the execution
    private void debounceUpdateStyles() {
        if (debounceTimer != null) {
            debounceTimer.cancel();
        }
        debounceTimer = new Timer();
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(MarkdownEditor.this::applyStyles);  // Apply styles asynchronously
            }
        }, 300); // Delay of 300 ms
    }

    // Applies styling based on markdown syntax
    private void applyStyles() {
        String text;
        StyledDocument doc = this.markdownPane.getStyledDocument();
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return;
        }
        
        doc.setCharacterAttributes(0, text.length(), doc.getStyle("default"), true);
        String[] lines = text.split("\n");
        int startOffset = 0;
        
        for (String line : lines) {
            int lineLength = line.length();
            if (line.startsWith("# ")) {
                doc.setCharacterAttributes(startOffset, lineLength, this.header1Style, true);
            } else if (line.startsWith("## ")) {
                doc.setCharacterAttributes(startOffset, lineLength, this.header2Style, true);
            } else if (line.startsWith("> ")) {
                doc.setCharacterAttributes(startOffset, lineLength, this.blockquoteStyle, true);
            } else if (line.startsWith("|")) {
                applyTableStyle(doc, line, startOffset);
            } else if (line.startsWith("* ") || line.startsWith("- ") || line.startsWith("+ ")) {
                doc.setCharacterAttributes(startOffset, lineLength, this.listItemStyle, true);
            }
            applyInlineStyle(doc, line, startOffset, "**", this.boldStyle);
            applyInlineStyle(doc, line, startOffset, "*", this.italicStyle);
            startOffset += lineLength + 1;
        }
    }

    // Applies inline styles (like bold and italic) based on delimiters
    private void applyInlineStyle(StyledDocument doc, String line, int startOffset, String delimiter, Style style) {
        int start = line.indexOf(delimiter);
        while (start != -1) {
            int end = line.indexOf(delimiter, start + delimiter.length());
            if (end == -1) break;
            doc.setCharacterAttributes(startOffset + start, end - start + delimiter.length(), style, true);
            start = line.indexOf(delimiter, end + delimiter.length());
        }
    }

    // Applies table styling, differentiating between headers and regular cells
    private void applyTableStyle(StyledDocument doc, String line, int startOffset) {
        String[] rows = line.split("\n");
        for (String row : rows) {
            if (row.trim().isEmpty()) continue; // Skip empty rows
            String[] cells = row.split("\\|");
            for (int i = 0; i < cells.length; i++) {
                if (i == 0 || i == cells.length - 1) continue; // Skip outer pipes
                int cellStart = startOffset + row.indexOf(cells[i]);
                int cellLength = cells[i].length();
                if (row.contains("---")) {
                    doc.setCharacterAttributes(cellStart, cellLength, this.tableHeaderStyle, true);
                } else {
                    doc.setCharacterAttributes(cellStart, cellLength, this.tableStyle, true);
                }
            }
            startOffset += row.length() + 1; // Move to the next line
        }
    }

    // Adds a listener to notify of text changes
    public void addTextChangeListener(TextChangeListener listener) {
        textChangeListeners.add(listener);
    }

    // Notifies all listeners of a text change
    private void notifyTextChangeListeners() {
        for (TextChangeListener listener : textChangeListeners) {
            listener.onTextChange(markdownPane.getText());
        }
    }

    // Getter to get the text from the markdownpane.
    public JTextPane getMarkdownPane() {
        return markdownPane;
    }
    
    // Listener interface for text change events
    public interface TextChangeListener {
        void onTextChange(String newText);
    }
}
