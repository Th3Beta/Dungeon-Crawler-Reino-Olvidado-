package com.rpg.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Persists and retrieves the top-10 ranking in ranking.json
 */
@Service
public class RankingService {

    private static final String RANKING_FILE = "ranking.json";
    private static final int MAX_ENTRIES     = 10;
    private static final ObjectMapper MAPPER  = new ObjectMapper();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public List<RankingEntry> getAll() {
        File file = new File(RANKING_FILE);
        if (!file.exists()) return new ArrayList<>();
        try {
            return MAPPER.readValue(file, new TypeReference<List<RankingEntry>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void submit(WebGame game) {
        String date = LocalDateTime.now().format(FMT);
        RankingEntry entry = new RankingEntry(
                game.getPlayerName(),
                game.getPlayerClass(),
                game.getFloor(),
                game.getTurn(),
                game.getLevel(),
                game.getScore(),
                date
        );

        List<RankingEntry> list = getAll();
        list.add(entry);
        list.sort(Comparator.comparingInt((RankingEntry e) -> e.score).reversed());
        if (list.size() > MAX_ENTRIES) {
            list = list.subList(0, MAX_ENTRIES);
        }

        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(RANKING_FILE), list);
        } catch (IOException e) {
            System.err.println("Error saving ranking: " + e.getMessage());
        }
    }
}
