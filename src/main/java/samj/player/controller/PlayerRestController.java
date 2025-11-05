package samj.player.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import samj.player.service.PlayerService;

@RestController
@RequestMapping
@CommonsLog
@RequiredArgsConstructor
public class PlayerRestController {
	private final PlayerService playerService;

	@GetMapping("/init-name")
	public ResponseEntity<String> initName() {
		log.info("/init-name");
		try {
			String randName = playerService.genRandomUserName();
			return ResponseEntity.ok(randName);
		} catch (InterruptedException e) {
			return ResponseEntity.ok("$err:Interrupted");
		} catch (Exception e) {
			return ResponseEntity.ok("$err:Exception:" + e.getMessage());
		}
	}
}
