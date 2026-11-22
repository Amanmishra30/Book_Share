package com.bookshare.ws;

import com.bookshare.service.ChatAccessService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/** Relays WebRTC signalling (offer / answer / ice / join / leave) between members of one reading room. */
@Controller
public class SignalingController {
    private final SimpMessagingTemplate ws;
    private final ChatAccessService access;

    public SignalingController(SimpMessagingTemplate ws, ChatAccessService access) {
        this.ws = ws;
        this.access = access;
    }

    @MessageMapping("/reading/{id}/signal")
    public void signal(@DestinationVariable Long id, @Payload Map<String, Object> payload, Principal principal) {
        Long uid = Long.valueOf(principal.getName());
        if (!access.canAccessReading(uid, id)) return;
        Map<String, Object> out = new HashMap<>(payload);
        out.put("from", uid);
        ws.convertAndSend("/topic/reading." + id + ".signal", out);
    }
}
