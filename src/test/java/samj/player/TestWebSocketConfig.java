package samj.player;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import samj.player.model.Board;
import samj.player.service.PlayerService;
import samj.player.util.JacksonMapper;
import samj.player.ws.PlayerWebSocketHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class TestWebSocketConfig {

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

}
