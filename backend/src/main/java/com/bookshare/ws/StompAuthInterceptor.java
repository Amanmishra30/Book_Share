package com.bookshare.ws;

import com.bookshare.security.AuthUser;
import com.bookshare.security.JwtService;
import com.bookshare.service.ChatAccessService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/** Authenticates STOMP CONNECT frames with the JWT and authorises every SUBSCRIBE. */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    private final JwtService jwt;
    private final ChatAccessService access;

    public StompAuthInterceptor(JwtService jwt, ChatAccessService access) {
        this.jwt = jwt;
        this.access = access;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() == null) return message;
        StompCommand cmd = acc.getCommand();

        if (StompCommand.CONNECT.equals(cmd)) {
            String h = acc.getFirstNativeHeader("Authorization");
            AuthUser u = (h != null && h.startsWith("Bearer ")) ? jwt.parse(h.substring(7)) : null;
            if (u == null) throw new MessagingException("Unauthorized");
            acc.setUser(new UsernamePasswordAuthenticationToken(u, null, List.of()));
        } else if (StompCommand.SUBSCRIBE.equals(cmd)) {
            Principal p = acc.getUser();
            if (p == null) throw new MessagingException("Unauthorized");
            if (!access.canSubscribe(Long.valueOf(p.getName()), acc.getDestination())) {
                throw new MessagingException("Forbidden");
            }
        } else if (StompCommand.SEND.equals(cmd) && acc.getUser() == null) {
            throw new MessagingException("Unauthorized");
        }
        return message;
    }
}
