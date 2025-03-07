package com.github.aboodRS.collaborative_markdown_editor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
* The main method serves as the entry point for the Spring Boot application.
* It triggers the Spring application context to start, initializing all 
* beans and components, including the WebSocket configuration and handlers.
*/
@SpringBootApplication
public class CollaborativeMarkdownEditorApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollaborativeMarkdownEditorApplication.class, args);
    }
}
