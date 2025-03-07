package com.github.aboodRS.collaborative_markdown_editor_server;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension; // Ensure this import is present
import org.commonmark.ext.gfm.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;  

public class MarkdownPreview extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private JEditorPane previewPane; // Pane to render HTML content generated from Markdown
    private Parser parser; // CommonMark parser to parse markdown text
    private HtmlRenderer renderer; // Renders the parsed markdown into HTML

    public MarkdownPreview() {
        setLayout(new BorderLayout());
        
        // Initialize previewPane as a JEditorPane to render HTML
        previewPane = new JEditorPane();
        previewPane.setContentType("text/html"); // Set content type to HTML for rendering
        previewPane.setEditable(false); // Make it read-only
        
        //Design for the EditorPane
        previewPane.setBackground(new Color(40, 44, 52));
        previewPane.setForeground(Color.WHITE);
        
     // Honor font properties
        previewPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        previewPane.setFont(new Font("SansSerif", 0, 20));
        add((Component)new JScrollPane(this.previewPane), "Center"); // Add preview pane with scroll functionality
        
        // Initialize the CommonMark parser with table extension support (The CommonMark library does not have built-in Tables so an extention is needed)
        parser = Parser.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();
        // Initialize HTML renderer with table extension
        renderer = HtmlRenderer.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();

    }

    // Updates the preview pane with rendered HTML from Markdown text
    public void updatePreview(String markdownText) {
        // Custom CSS for styling tables and other elements
        String customStyles = "<style>" +
                "table { " +
                "   border-collapse: collapse; " +
                "   width: 100%; " +
                "} " +
                "th, td { " +
                "   border: 1px solid #ffffff; " + // Border color
                "   padding: 8px; " +
                "   text-align: left; " +
                "} " +
                "th { " +
                "   background-color: #333; " + // Header background
                "} " +
                "</style>";
        
        // Parse markdown and render it as HTML, then add custom styles
        String html = customStyles + renderer.render(parser.parse(markdownText));
        previewPane.setText(html); // Set the rendered HTML as the text of previewPane
    }
}