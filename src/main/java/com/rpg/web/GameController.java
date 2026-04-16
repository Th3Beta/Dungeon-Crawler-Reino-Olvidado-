package com.rpg.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller — exposes all game endpoints consumed by the frontend.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GameController {

    private final Map<String, WebGame> sessions = new ConcurrentHashMap<>();

    @Autowired
    private RankingService rankingService;

    // ── POST /api/game/new ────────────────────────────────────────────────────
    @PostMapping("/game/new")
    public ResponseEntity<Map<String, Object>> newGame(@RequestBody Map<String, String> body) {
        String playerClass = body.getOrDefault("playerClass", "warrior");
        String playerName  = body.getOrDefault("playerName",  "Héroe");

        String gameId = UUID.randomUUID().toString();
        WebGame game  = new WebGame(playerClass, playerName);
        sessions.put(gameId, game);

        return ResponseEntity.ok(Map.of(
                "gameId", gameId,
                "state",  game.buildState()
        ));
    }

    // ── POST /api/game/{id}/action ────────────────────────────────────────────
    @PostMapping("/game/{id}/action")
    public ResponseEntity<?> action(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        WebGame game = sessions.get(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        String action = body.getOrDefault("action", "");
        GameStateDTO state = game.processAction(action);

        // Auto-submit to ranking on death or save
        if (state.status.equals("DEAD") || state.status.equals("SAVED")) {
            rankingService.submit(game);
        }

        return ResponseEntity.ok(state);
    }

    // ── GET /api/game/{id}/state ──────────────────────────────────────────────
    @GetMapping("/game/{id}/state")
    public ResponseEntity<?> getState(@PathVariable String id) {
        WebGame game = sessions.get(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game.buildState());
    }

    // ── GET /api/ranking ──────────────────────────────────────────────────────
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingEntry>> getRanking() {
        return ResponseEntity.ok(rankingService.getAll());
    }
}
