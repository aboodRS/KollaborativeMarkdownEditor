package com.github.aboodRS.collaborative_markdown_editor;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MyWebSocketHandler extends TextWebSocketHandler {
    // Maps session IDs to their connected WebSocket sessions
    private Map<String, List<WebSocketSession>> sessionClients = new HashMap<>();
    // Stores hashed passwords for each session
    private Map<String, String> sessionPasswords = new HashMap<>(); 

    /**
     * This method is called when a new client connection is established.
     * It extracts the session ID from the session's URI and adds the session
     * to the corresponding list of connected clients, allowing for message 
     * routing within that session.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        sessionClients.putIfAbsent(sessionId, new ArrayList<>());
        sessionClients.get(sessionId).add(session);
        System.out.println("Client connected to session " + sessionId + ": " + session.getId());
    }

    /**
     * This method handles incoming text messages from connected clients.
     * It processes the message to either set a password for the session or
     * allow a user to join a session by verifying the provided password.
     * If the password verification is successful, it allows the user to join
     * the session; otherwise, it sends an error message and closes the session.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        System.out.println("Received message in session " + sessionId + ": " + message.getPayload());

        // Check for password verification
        String[] messageParts = message.getPayload().split(":", 2); // Expected format: "action:password"
        String action = messageParts[0];
        if ("setPassword".equals(action) && messageParts.length > 1) {
            String password = messageParts[1];
            String hashedPassword = hashPassword(password); // Hash the password before storing it
            sessionPasswords.put(sessionId, hashedPassword);
            session.sendMessage(new TextMessage(sessionId));
            return;
        } else if ("join".equals(action) && messageParts.length > 1) {
            String password = messageParts[1];
            String hashedPassword = hashPassword(password);
            String storedPassword = sessionPasswords.get(sessionId);
            // Validate password
            if (storedPassword != null && storedPassword.equals(hashedPassword)) {
                session.sendMessage(new TextMessage("Successfully joined session " + sessionId));
                return;
            } else {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("SYSTEM:Incorrect password for session " + sessionId));
                }
                session.close(); // Close session if password is incorrect
                }
            return;
        }
        // Forward the message to all other clients in the same session
        for (WebSocketSession s : sessionClients.get(sessionId)) {
            if (s.isOpen() && !s.getId().equals(session.getId())) {
                s.sendMessage(message);
            }
        }
    }

        /**
     * This method is called when a client connection is closed.
     * It removes the corresponding session from the list of connected clients,
     * ensuring that the session tracking remains accurate and does not hold
     * references to closed sessions.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session); // Extract session ID from the session's URI
        sessionClients.get(sessionId).remove(session);  // Remove the session from the client list
        System.out.println("Client disconnected from session " + sessionId + ": " + session.getId());
    }

    /**
     * This helper method extracts the session ID from the session's URI,
     * allowing the application to identify which session the WebSocket
     * connection belongs to.
     */
    private String extractSessionId(WebSocketSession session) {
        String uri = session.getUri().toString();
        return uri.substring(uri.lastIndexOf('/') + 1); // Extract session ID from URI
    }

    /**
     * This method hashes the given password using SHA-256 and encodes it
     * in Base64 format. It is used for securely storing passwords without
     * exposing them in plain text.
     */
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes());  // Generate the password hash
        return Base64.getEncoder().encodeToString(hash); // Encode hash to a readable string
    }
}
