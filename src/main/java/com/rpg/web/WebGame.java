package com.rpg.web;

import Characters.Character;
import Characters.Player;
import Characters.Enemy;
import Characters.factory.PlayerFactory;
import Inventory.Inventory;
import Inventory.Item;
import Inventory.Potion;
import Map.Map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Engine de juego sin Scanner — toda la I/O se hace a través de este objeto.
 * Sustituye a Game.java para el contexto web.
 */
public class WebGame {

    // ── Estado del juego ──────────────────────────────────────────────────────
    private Player activePlayer;
    private Map map;
    private Inventory inventory;
    private int turnCount;
    private int currentFloor;
    private String lastEvent;
    private GameStatus status;

    // ── Estado de combate activo ──────────────────────────────────────────────
    private Enemy currentEnemy;
    private int enemyPoisonTurns;
    private int playerPoisonTurns;

    // ── Buffer de log de mensajes ─────────────────────────────────────────────
    private final List<String> eventLog = new ArrayList<>();

    private static final int POISON_TURNS  = 3;
    private static final int POISON_DAMAGE = 6;
    private static final double POISON_CHANCE = 0.20;
    private static final Random RNG = new Random();

    public enum GameStatus { PLAYING, IN_COMBAT, DEAD, SAVED }

    // ── Constructor ───────────────────────────────────────────────────────────

    public WebGame(String playerClass, String playerName) {
        inventory    = new Inventory(10);
        turnCount    = 1;
        currentFloor = 1;
        status       = GameStatus.PLAYING;
        map          = new Map(25, currentFloor);

        try {
            activePlayer = PlayerFactory.create(playerClass, playerName.isBlank() ? "Héroe" : playerName);
        } catch (IllegalArgumentException e) {
            activePlayer = PlayerFactory.create("warrior", playerName.isBlank() ? "Héroe" : playerName);
        }

        setEvent("¡" + activePlayer.getName() + " inicia su aventura!");
        log("Partida iniciada. ¡Que comience la aventura!");
    }

    // ── Acción principal ──────────────────────────────────────────────────────

    /**
     * Procesa una acción del jugador y devuelve el estado actualizado.
     * @param action  move_w | move_a | move_s | move_d | attack | special | use_item:N | save | quit
     */
    public GameStateDTO processAction(String action) {
        if (status == GameStatus.DEAD || status == GameStatus.SAVED) {
            return buildState();
        }

        if (status == GameStatus.IN_COMBAT) {
            handleCombatAction(action);
        } else {
            handleExplorationAction(action);
        }

        return buildState();
    }

    // ── Exploración ───────────────────────────────────────────────────────────

    private void handleExplorationAction(String action) {
        switch (action) {
            case "move_w" -> doMove('w');
            case "move_a" -> doMove('a');
            case "move_s" -> doMove('s');
            case "move_d" -> doMove('d');
            case "save"   -> { status = GameStatus.SAVED; setEvent("Partida guardada."); }
            case "quit"   -> { status = GameStatus.DEAD;  setEvent("Abandonaste la aventura."); }
            default -> {
                if (action.startsWith("use_item:")) {
                    int idx = Integer.parseInt(action.split(":")[1]);
                    useItem(idx);
                }
            }
        }
    }

    private void doMove(char dir) {
        map.movePlayer(dir);
        handleChestLoot();

        if (!activePlayer.isAlive()) {
            status = GameStatus.DEAD;
            setEvent("Has caído en batalla...");
            return;
        }

        if (map.isOnStairs()) {
            currentFloor++;
            map = new Map(25, currentFloor);
            setEvent("⚡ ¡Desciendes al Piso " + currentFloor + "! El peligro crece...");
            log("► Piso " + currentFloor + " alcanzado.");
            return;
        }

        // Comprobar combate después de moverse
        Enemy enemy = map.checkEnemy();
        if (enemy != null) {
            startCombat(enemy);
            return;
        }

        // Turno del mundo (enemigos se mueven)
        map.moveEnemies();

        // Comprobar si un enemigo entró en la casilla del jugador
        enemy = map.checkEnemy();
        if (enemy != null) {
            startCombat(enemy);
            return;
        }

        turnCount++;
    }

    private void handleChestLoot() {
        Item loot = map.collectChestLoot();
        if (loot == null) return;
        inventory.addItem(loot);
        setEvent("🎁 ¡Cofre abierto! Obtuviste: " + loot.getName());
        log("Cofre → " + loot.getName());
    }

    private void useItem(int index) {
        if (index < 0 || index >= inventory.getSize()) {
            setEvent("Ítem inválido.");
            return;
        }
        inventory.useItem(index, activePlayer);
        setEvent("Usaste un ítem.");
    }

    // ── Combate ───────────────────────────────────────────────────────────────

    private void startCombat(Enemy enemy) {
        currentEnemy = enemy;
        enemyPoisonTurns  = 0;
        playerPoisonTurns = 0;
        status = GameStatus.IN_COMBAT;
        setEvent("⚔️ ¡Encuentro hostil con " + enemy.getName() + "!");
        log("Combate iniciado contra " + enemy.getName());
    }

    private void handleCombatAction(String action) {
        if (currentEnemy == null || !currentEnemy.isAlive()) {
            endCombatVictory();
            return;
        }

        // ── Veneno al inicio del turno ────────────────────────────────────────
        if (enemyPoisonTurns > 0) {
            enemyPoisonTurns = applyPoison(currentEnemy, enemyPoisonTurns);
            if (!currentEnemy.isAlive()) { endCombatVictory(); return; }
        }
        if (playerPoisonTurns > 0) {
            playerPoisonTurns = applyPoison(activePlayer, playerPoisonTurns);
            if (!activePlayer.isAlive()) { endCombatDefeat(); return; }
        }

        // ── Turno del jugador ─────────────────────────────────────────────────
        boolean enemyAttacks = true; // si el jugador no actua (ítem), el enemigo no ataca

        if (action.equals("attack")) {
            int dmg = activePlayer.attack();
            currentEnemy.takeDamage(dmg);
            log("⚔ Atacas a " + currentEnemy.getName() + " por " + dmg + " daño.");
            setEvent("⚔ ¡Atacas a " + currentEnemy.getName() + " por " + dmg + " daño!");
            if (currentEnemy.isAlive() && RNG.nextDouble() < POISON_CHANCE) {
                enemyPoisonTurns = Math.max(enemyPoisonTurns, POISON_TURNS);
                log(currentEnemy.getName() + " queda envenenado.");
            }

        } else if (action.equals("special")) {
            if (!activePlayer.canUseSpecial()) {
                setEvent("☁️ Maná insuficiente (" + activePlayer.getMana()
                        + "/" + activePlayer.getSpecialManaCost() + "). Pierdes el turno.");
            } else {
                try {
                    activePlayer.special(currentEnemy);
                    String skillName = getSpecialName(activePlayer.getClass().getSimpleName());
                    setEvent("✨ ¡Usas " + skillName + "!");
                    log("✨ " + skillName + " ejecutado.");
                } catch (IllegalStateException e) {
                    setEvent("☁️ " + e.getMessage());
                }
            }

        } else if (action.startsWith("use_item:")) {
            int idx = Integer.parseInt(action.split(":")[1]);
            useItem(idx);
            enemyAttacks = false; // usar ítem no provoca contraataque

        } else {
            setEvent("Acción de combate no reconocida.");
            return;
        }

        // ── Comprobar si el enemigo murió por el ataque del jugador ───────────
        if (!currentEnemy.isAlive()) {
            endCombatVictory();
            return;
        }

        // ── Turno del enemigo ─────────────────────────────────────────────────
        if (enemyAttacks) {
            int enemyDmg = currentEnemy.attack();
            activePlayer.takeDamage(enemyDmg);
            log(currentEnemy.getName() + " te ataca por " + enemyDmg + " daño.");
            if (activePlayer.isAlive() && RNG.nextDouble() < POISON_CHANCE * 0.5) {
                playerPoisonTurns = Math.max(playerPoisonTurns, POISON_TURNS);
                setEvent("☠️ " + currentEnemy.getName() + " te envenena! (" + enemyDmg + " daño)");
            } else {
                setEvent("🛡️ " + currentEnemy.getName() + " contraataca por " + enemyDmg + " daño.");
            }

            if (!activePlayer.isAlive()) {
                endCombatDefeat();
            }
        }
    }

    private String getSpecialName(String cls) {
        return switch (cls) {
            case "Warrior" -> "Golpe Aplastante";
            case "Mage"    -> "Bola de Fuego";
            case "Archer"  -> "Lluvia de Flechas";
            default        -> "Habilidad Especial";
        };
    }

    private int applyPoison(Character target, int turns) {
        target.takeDamage(POISON_DAMAGE);
        log(target.getName() + " sufre " + POISON_DAMAGE + " de veneno. (" + (turns-1) + " turnos restantes)");
        return turns - 1;
    }

    private void endCombatVictory() {
        map.removeEnemy();
        inventory.addItem(new Potion("Poción menor", 30));
        activePlayer.gainExp(50);
        currentEnemy = null;
        status = GameStatus.PLAYING;
        turnCount++;
        setEvent("💀 ¡Victoria! +50 EXP y una Poción menor.");
        log("Combate ganado.");
    }

    private void endCombatDefeat() {
        status = GameStatus.DEAD;
        currentEnemy = null;
        setEvent("☠️ Has sido derrotado. Fin de la aventura.");
        log("El jugador ha muerto.");
    }

    // ── Construcción del DTO ──────────────────────────────────────────────────

    public GameStateDTO buildState() {
        GameStateDTO dto = new GameStateDTO();

        // HUD
        dto.playerName  = activePlayer.getName();
        dto.playerClass = activePlayer.getClass().getSimpleName();
        dto.hp          = activePlayer.getHp();
        dto.maxHp       = activePlayer.getMaxHp();
        dto.mana        = activePlayer.getMana();
        dto.maxMana     = activePlayer.getMaxMana();
        dto.level       = activePlayer.getLevel();
        dto.exp         = activePlayer.getExp();
        dto.floor       = currentFloor;
        dto.turn        = turnCount;
        dto.lastEvent   = lastEvent;
        dto.status      = status.name();
        dto.weapon      = activePlayer.getEquippedWeapon() != null
                            ? activePlayer.getEquippedWeapon().getName() : null;
        dto.armor       = activePlayer.getEquippedArmor() != null
                            ? activePlayer.getEquippedArmor().getName() : null;

        // Score
        dto.score = (currentFloor * 1000) + (turnCount * 10) + (activePlayer.getLevel() * 500);

        // Mapa
        dto.mapGrid = map.getGridForWeb();

        // Inventario
        dto.inventoryItems = inventory.getItemNames();

        // Combate activo
        if (status == GameStatus.IN_COMBAT && currentEnemy != null) {
            dto.enemyName  = currentEnemy.getName();
            dto.enemyHp    = currentEnemy.getHp();
            dto.enemyMaxHp = currentEnemy.getMaxHp();
        }

        // Log de eventos recientes
        dto.recentLog = eventLog.size() > 8
                ? eventLog.subList(eventLog.size() - 8, eventLog.size())
                : new ArrayList<>(eventLog);

        return dto;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private void setEvent(String msg) {
        this.lastEvent = msg;
        log(msg);
    }

    private void log(String msg) {
        eventLog.add(msg);
    }

    // ── Getters para el ranking ───────────────────────────────────────────────

    public String getPlayerName()  { return activePlayer.getName(); }
    public String getPlayerClass() { return activePlayer.getClass().getSimpleName(); }
    public int getFloor()          { return currentFloor; }
    public int getTurn()           { return turnCount; }
    public int getLevel()          { return activePlayer.getLevel(); }
    public int getScore()          { return (currentFloor * 1000) + (turnCount * 10) + (activePlayer.getLevel() * 500); }
    public GameStatus getStatus()  { return status; }
}
