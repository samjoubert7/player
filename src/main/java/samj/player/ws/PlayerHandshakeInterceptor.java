package samj.player.ws;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class PlayerHandshakeInterceptor implements HandshakeInterceptor {

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
		/*
        // Example: Transfer an attribute from the HTTP session
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession httpSession = servletRequest.getServletRequest().getSession();
            if (httpSession != null) {
                attributes.put("httpSessionId", httpSession.getId());
            }
        }
        */
        // Add custom attributes
		log.info("ServerHttpRequest query " + request.getURI().getQuery());
		String query = request.getURI().getQuery();
		String[] params = query.split("&");
		for (String param : params) {
			String[] parts = param.split("=");
			if ("name".equals(parts[0]) && parts.length > 0) {
				attributes.put("playerName", parts[1]);
			}
		}
        return true;
    }

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
		// TODO Auto-generated method stub
		
	}

}
