package samj.player.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import samj.player.model.Board;
import samj.player.service.PlayerService;
import samj.player.util.JacksonMapper;
import samj.player.ws.PlayerWebSocketHandler;
import samj.player.ws.PlayerHandshakeInterceptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@CommonsLog
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean("mapper")
    JacksonMapper getMapper() {
    	return new JacksonMapper();
    }
    
    @Bean("virtualThreadExecutor")
	ExecutorService getVirtualThreadExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
    
    @Bean("board")
    Board getBoard() {
    	return new Board();
    }

    @Bean("playerService")
    PlayerService getPlayerService() {
    	return new PlayerService(getVirtualThreadExecutor(), getMapper(), getBoard());
    }
    
    @Bean("customWebSocketHandler")
    PlayerWebSocketHandler getCustomWebSocketHandler() {
    	PlayerService playerService = getPlayerService();
    	PlayerWebSocketHandler handler = new PlayerWebSocketHandler(getVirtualThreadExecutor(), getMapper(), playerService);
    	playerService.setCustomWebSocketHandler(handler);
    	return handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    	log.info("registerWebSocketHandlers");
        registry.addHandler(getCustomWebSocketHandler(), "/ws/notifications")
        		.addInterceptors(new PlayerHandshakeInterceptor());
    }

}
