package com.posturecoach;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PostureSocketHandler extends BinaryWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(PostureSocketHandler.class);
    private static final Duration BAD_POSTURE_ALERT = Duration.ofSeconds(20);
    private static final Duration EYE_BREAK = Duration.ofMinutes(20); // 20-20-20 rule
    private static final Duration ALERT_COOLDOWN = Duration.ofMinutes(1);

    private final PoseService pose;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    static class Session {
        Instant lastBreak = Instant.now(), badSince, lastAlert = Instant.EPOCH;
    }

    public PostureSocketHandler(PoseService pose) { this.pose = pose; }

    @Override
    public void afterConnectionEstablished(WebSocketSession s) { sessions.put(s.getId(), new Session()); }

    @Override
    public void afterConnectionClosed(WebSocketSession s, CloseStatus st) { sessions.remove(s.getId()); }

    @Override
    protected void handleBinaryMessage(WebSocketSession ws, BinaryMessage msg) throws Exception {
        Session st = sessions.computeIfAbsent(ws.getId(), k -> new Session());
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            byte[] bytes = new byte[msg.getPayloadLength()];
            msg.getPayload().get(bytes);
            PostureMetrics m = pose.analyze(bytes);
            Instant now = Instant.now();
            out.put("metrics", m);

            String alert = null;
            if (m.detected() && !m.issues().isEmpty()) {
                if (st.badSince == null) st.badSince = now;
                if (Duration.between(st.badSince, now).compareTo(BAD_POSTURE_ALERT) >= 0
                        && Duration.between(st.lastAlert, now).compareTo(ALERT_COOLDOWN) >= 0) {
                    alert = "POSTURE";
                }
            } else if (m.detected()) {
                st.badSince = null;
            }
            if (Duration.between(st.lastBreak, now).compareTo(EYE_BREAK) >= 0) {
                alert = "EYE_BREAK";
                st.lastBreak = now;
            }
            if (alert != null) { st.lastAlert = now; out.put("alert", alert); }
        } catch (Exception e) {
            log.warn("analysis failed", e);
            out.put("error", e.getMessage());
        }
        ws.sendMessage(new TextMessage(mapper.writeValueAsString(out)));
    }
}
