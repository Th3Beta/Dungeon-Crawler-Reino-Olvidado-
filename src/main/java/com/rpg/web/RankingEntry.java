package com.rpg.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a single entry in the ranking leaderboard.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RankingEntry {

    public String playerName;
    public String playerClass;
    public int    floor;
    public int    turn;
    public int    level;
    public int    score;
    public String date;

    public RankingEntry() {}

    public RankingEntry(String playerName, String playerClass,
                        int floor, int turn, int level, int score, String date) {
        this.playerName  = playerName;
        this.playerClass = playerClass;
        this.floor       = floor;
        this.turn        = turn;
        this.level       = level;
        this.score       = score;
        this.date        = date;
    }
}
