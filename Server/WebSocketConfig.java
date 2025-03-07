package com.github.aboodRS.collaborative_markdown_editor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
* This method is used to register WebSocket handlers with the specified URL pattern.
* It maps the MyWebSocketHandler to the URL path "/collaborate/{sessionId}", allowing
* clients to connect to specific collaborative sessions using their session IDs.
* The setAllowedOrigins method is called to permit cross-origin requests, 
* which is necessary for client applications hosted on different origins.
*/
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MyWebSocketHandler(), "/collaborate/{sessionId}")
                .setAllowedOrigins("*");
    }
}
