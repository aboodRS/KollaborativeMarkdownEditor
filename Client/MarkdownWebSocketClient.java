package com.github.aboodRS.collaborative_markdown_editor_server;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.function.Consumer;
import java.net.URI;

public class MarkdownWebSocketClient {
    private WebSocketSession session; // The WebSocket session for the connection
    private Consumer<String> messageHandler; // Function to handle incoming messages
    
    // Constructor initializes the WebSocket client and connects to the specified URI
    public MarkdownWebSocketClient(String uri) throws Exception {
    	// Create a standard WebSocket client
        WebSocketClient client = new StandardWebSocketClient(); 
        
        // Establish the WebSocket handshake and define the message handling
        session = client.doHandshake(new TextWebSocketHandler() { 
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                // Handle incoming message
                if (messageHandler != null) { // Check if a message handler is set
                    messageHandler.accept(message.getPayload()); // Pass the message payload to the handler
                }
                System.out.println("Received message: " + message.getPayload()); // Log the received message
            }
        }, uri).get();  // Connect to the WebSocket server at the specified URI
    }

    // Set the message handler for incoming messages
    // This function allows users to define custom actions for handling messages 
    // received from the WebSocket server, enabling flexible response behavior.
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    // Send a message to the WebSocket server
    // This function facilitates real-time communication by allowing the client to 
    // send updates to the server, ensuring that other clients can receive and 
    // reflect those changes promptly.
    public void send(String message) throws Exception {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }
    
    // Close the WebSocket session
    // This function is important for resource management, ensuring that the WebSocket 
    // connection is properly closed when it's no longer needed, preventing resource leaks 
    // and maintaining application stability.
    public void close() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();  // Close the WebSocket session
            System.out.println("WebSocket session closed.");
        }
    }
}
