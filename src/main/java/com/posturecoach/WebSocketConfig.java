package com.posturecoach;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final PostureSocketHandler handler;

    public WebSocketConfig(PostureSocketHandler handler) { this.handler = handler; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/posture").setAllowedOriginPatterns("*");
    }

    @Bean
    public ServletServerContainerFactoryBean wsContainer() {
        ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
        c.setMaxBinaryMessageBufferSize(2 * 1024 * 1024); // JPEG snapshots
        return c;
    }
}
