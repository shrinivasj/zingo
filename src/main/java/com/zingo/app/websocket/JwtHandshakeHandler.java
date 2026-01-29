package com.zingo.app.websocket;

import com.zingo.app.security.JwtService;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {
  private final JwtService jwtService;

  public JwtHandshakeHandler(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
    try {
      if (request instanceof ServletServerHttpRequest servletRequest) {
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null) {
          String auth = servletRequest.getServletRequest().getHeader("Authorization");
          if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
          }
        }
        if (token != null && !token.isBlank()) {
          JwtService.JwtUser jwtUser = jwtService.parseToken(token);
          return new StompPrincipal(String.valueOf(jwtUser.userId()));
        }
      }
    } catch (Exception ignored) {
      // fall through
    }
    return new StompPrincipal("anonymous");
  }
}
