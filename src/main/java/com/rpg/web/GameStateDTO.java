package com.rpg.web;

import java.util.List;

/**
 * DTO que representa el estado completo del juego para el frontend.
 * Se serializa automáticamente a JSON por Jackson.
 */
public class GameStateDTO {

    // ── HUD ───────────────────────────────────────────────────────────────────
    public String playerName;
    public String playerClass;
    public int    hp;
    public int    maxHp;
    public int    mana;
    public int    maxMana;
    public int    level;
    public int    exp;
    public int    floor;
    public int    turn;
    public String lastEvent;
    public String status;   // PLAYING | IN_COMBAT | DEAD | SAVED
    public String weapon;
    public String armor;
    public int    score;

    // ── Mapa ─────────────────────────────────────────────────────────────────
    /**
     * Grid 25×25. Cada celda es un String con dos campos separados por ':'
     * Formato: "TIPO:FOV"  donde FOV es 'true' | 'false' | 'dim'
     * Tipos: PLAYER, ENEMY, BOSS, CHEST, FLOOR, GRASS, TREE_BIG, TREE_SMALL,
     *        MOUNTAIN, WATER, STAIRS, UNDISCOVERED
     */
    public String[][] mapGrid;

    // ── Inventario ─────────────────────────────────────────────────────────────
    public List<String> inventoryItems;

    // ── Combate activo ────────────────────────────────────────────────────────
    public String enemyName;
    public int    enemyHp;
    public int    enemyMaxHp;

    // ── Log ───────────────────────────────────────────────────────────────────
    public List<String> recentLog;
}
