import Characters.*;
import Characters.factory.PlayerFactory;
import Map.Map;
import Combat.Combat;
import Inventory.Inventory;
import Inventory.Item;
import Inventory.Potion;
import Management.Party;

import java.util.Scanner;

import java.io.Serializable;

public class Game implements Serializable {
    // =========================================================
    //  ESTILO VISUAL DE INTERFAZ (ANSI)
    // =========================================================

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String WHITE = "\u001B[37m";
    private static final String DIM = "\u001B[2m";

    private Party party;
    private Player activePlayer;
    private Map map;
    private Inventory inventory;
    private transient Scanner sc;
    private int turnCount;
    private int currentFloor;
    private String lastEvent;

    public Game() {
        sc = new Scanner(System.in);
        inventory = new Inventory(10);
        party = new Party();
        turnCount = 1;
        currentFloor = 1;
        map = new Map(25, currentFloor);
        setEvent("Explora el reino y sobrevive.");

        partyManagementMenu();
    }

    public void restoreTransientState() {
        if (sc == null) {
            sc = new Scanner(System.in);
        }
    }

    // =========================================================
    //  MENÚ DE GESTIÓN DE GRUPO
    // =========================================================

    private void partyManagementMenu() {
        int option = 0;
        do {
            System.out.println("\n=== GESTIÓN DE GRUPO ===");
            System.out.println("1. Añadir personaje");
            System.out.println("2. Eliminar personaje");
            System.out.println("3. Listar personajes por clase");
            System.out.println("4. Empezar aventura (Requiere al menos 1 personaje)");

            option = readIntInRange(1, 4);

            switch (option) {
                case 1 -> createCharacter();
                case 2 -> {
                    System.out.print("Introduce el nombre del personaje a eliminar: ");
                    String nameToRemove = sc.nextLine();
                    party.removeCharacter(nameToRemove);
                }
                case 3 -> {
                    System.out.print("Introduce la clase a buscar (Warrior, Mage, Archer): ");
                    String typeToSearch = sc.nextLine();
                    party.listCharactersByType(typeToSearch);
                }
                case 4 -> {
                    if (party.isEmpty()) {
                        System.out.println("¡No puedes empezar sin personajes! Añade uno primero.");
                        option = 0; // fuerza continuar el bucle
                    }
                }
            }

        } while (option != 4);

        activePlayer = party.getActivePlayer();
    }

    // =========================================================
    //  CREACIÓN DE PERSONAJE (usa PlayerFactory)
    // =========================================================

    private void createCharacter() {
        System.out.println("\nElige tu clase:");
        System.out.println("  1 - Warrior  (HP: 120 | ATK: 20 | DEF: 10) - Tanque cuerpo a cuerpo");
        System.out.println("  2 - Mage     (HP:  80 | ATK: 15 | DEF:  5) - Daño mágico explosivo");
        System.out.println("  3 - Archer   (HP:  90 | ATK: 18 | DEF:  6) - Equilibrado y veloz");

        int choice = readIntInRange(1, 3);
        String[] types = {"warrior", "mage", "archer"};

        System.out.print("Nombre de tu personaje: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "Héroe";

        try {
            Player newPlayer = PlayerFactory.create(types[choice - 1], name);
            party.addCharacter(newPlayer);
        } catch (IllegalArgumentException e) {
            System.out.println("Error al crear personaje: " + e.getMessage());
        }
    }

    // =========================================================
    //  BUCLE PRINCIPAL DE JUEGO
    // =========================================================

    public void start() {
        System.out.println("\n" + BOLD + CYAN + "¡Comenzando aventura con " + activePlayer.getName() + "!" + RESET);

        char option;
        do {
            renderFrame();
            map.draw();
            printControls();
            option = readChar('w', 'a', 's', 'd', 'i', 'q', 'x');

            switch (option) {
                case 'i' -> {
                    handleInventory();
                    setEvent("Gestionaste el inventario.");
                }
                case 'x' -> {
                    System.out.println(YELLOW + "Guardando partida..." + RESET);
                    Management.FileManager.saveGame(this);
                    setEvent("Partida guardada.");
                    System.out.println(YELLOW + "Saliendo al menú principal..." + RESET);
                    return; // Retorna a Main
                }
                case 'q' -> {
                    setEvent("Abandonando aventura (progreso no guardado)...");
                    System.out.println(YELLOW + lastEvent + RESET);
                }
                default  -> handleMovement(option);
            }

            // Limpieza tras cada accion del jugador.
            clearScreen();

        } while (option != 'q' && activePlayer.isAlive());

        if (!activePlayer.isAlive()) {
            System.out.println("\n=== FIN DE LA PARTIDA ===");
            System.out.println(activePlayer.getName() + " ha caído en batalla. Fin del juego.");
        }
    }

    // =========================================================
    //  ACCIONES DEL BUCLE PRINCIPAL
    // =========================================================

    private void handleMovement(char direction) {
        map.movePlayer(direction);
        handleChestLoot();
        resolveCombatIfNeeded();

        if (activePlayer.isAlive() && map.isOnStairs()) {
            System.out.println(BOLD + CYAN + "¡Has encontrado las escaleras! Desciendes a las profundidades..." + RESET);
            currentFloor++;
            map = new Map(25, currentFloor);
            setEvent("Has bajado al Piso " + currentFloor + ".");
            return; // no movemos enemigos si bajamos
        }

        // Cada paso del jugador consume turno del mundo: mueven enemigos.
        if (activePlayer.isAlive()) {
            map.moveEnemies();
            resolveCombatIfNeeded(); // Por si un enemigo entra en tu casilla.
        }
        turnCount++;
    }

    private void handleChestLoot() {
        Item loot = map.collectChestLoot();
        if (loot == null) return;
        System.out.println(BOLD + YELLOW + "¡Has abierto un cofre!" + RESET);
        inventory.addItem(loot);
        setEvent("Botin conseguido: " + loot.getName());
    }

    private void resolveCombatIfNeeded() {
        Enemy enemy = map.checkEnemy();
        if (enemy == null) return;

        setEvent("Encuentro hostil con " + enemy.getName() + ".");
        Combat.fight(activePlayer, enemy, sc);
        if (!enemy.isAlive()) {
            map.removeEnemy();
            // Recompensa base por victoria.
            inventory.addItem(new Potion("Poción menor", 30));
            setEvent("Victoria contra " + enemy.getName() + ". +Pocion menor");
        } else if (!activePlayer.isAlive()) {
            setEvent("Has sido derrotado por " + enemy.getName() + ".");
        }
    }

    private void handleInventory() {
        if (inventory.isEmpty()) {
            System.out.println("El inventario está vacío.");
            return;
        }
        inventory.showItems();
        System.out.print("Usa un ítem (número) o 0 para cancelar: ");
        int choice = readIntInRange(0, inventory.getSize());
        if (choice > 0) {
            inventory.useItem(choice - 1, activePlayer);
        }
    }

    // =========================================================
    //  HUD (cabecera de estado del jugador)
    // =========================================================

    private void printHud() {
        int hp = activePlayer.getHp();
        int maxHp = activePlayer.getMaxHp();
        int mana = activePlayer.getMana();
        int maxMana = activePlayer.getMaxMana();

        System.out.println(BOLD + WHITE + "============================================================" + RESET);
        System.out.printf(BOLD + " Piso %d | Turno %d | %s%s%s | Nivel %d | EXP %d%n" + RESET,
                currentFloor, turnCount, CYAN, activePlayer.getName(), RESET, activePlayer.getLevel(), activePlayer.getExp());
        System.out.printf(" HP   %s %d/%d%n", makeBar(hp, maxHp, 20, GREEN, RED), hp, maxHp);
        System.out.printf(" Mana %s %d/%d%n", makeBar(mana, maxMana, 20, CYAN, WHITE), mana, maxMana);
        
        String equipInfo = "";
        if (activePlayer.getEquippedWeapon() != null) equipInfo += "🗡️ " + activePlayer.getEquippedWeapon().getName() + "  ";
        if (activePlayer.getEquippedArmor() != null) equipInfo += "🛡️ " + activePlayer.getEquippedArmor().getName();
        if (!equipInfo.isEmpty()) {
            System.out.println(" Equipo: " + DIM + equipInfo + RESET);
        }

        System.out.println(" Evento: " + YELLOW + lastEvent + RESET);
        System.out.println(BOLD + WHITE + "============================================================" + RESET);
    }

    private void renderFrame() {
        clearScreen();
        printHud();
    }

    private void printControls() {
        System.out.println(BOLD + "Comandos:" + RESET + " " +
                GREEN + "[w/a/s/d]" + RESET + " mover  |  " +
                CYAN + "[i]" + RESET + " inventario  |  " +
                YELLOW + "[x]" + RESET + " guardar y salir  |  " +
                RED + "[q]" + RESET + " abandonar");
        System.out.print("Accion > ");
    }

    private String makeBar(int current, int max, int width, String fillColor, String emptyColor) {
        if (max <= 0) return "[ ]";
        int fill = (int) Math.round((current / (double) max) * width);
        fill = Math.max(0, Math.min(width, fill));
        int empty = width - fill;
        return fillColor + "[" + "█".repeat(fill) + emptyColor + "░".repeat(empty) + fillColor + "]" + RESET;
    }

    private void clearScreen() {
        System.out.print("\u001B[H\u001B[2J");
        System.out.flush();
    }

    private void setEvent(String eventMsg) {
        this.lastEvent = eventMsg;
        Management.FileManager.logEvent(eventMsg);
    }

    // =========================================================
    //  UTILIDADES DE ENTRADA — Scanner a prueba de fallos
    // =========================================================

    /**
     * Lee un entero dentro del rango [min, max] inclusive.
     * No retorna hasta obtener una entrada válida.
     */
    private int readIntInRange(int min, int max) {
        while (true) {
            System.out.printf("Elige una opción (%d-%d): ", min, max);
            if (sc.hasNextInt()) {
                int value = sc.nextInt();
                sc.nextLine(); // limpiar el \n residual
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Fuera de rango. Debe ser entre %d y %d.%n", min, max);
            } else {
                System.out.println("Entrada inválida. Por favor, introduce un número.");
                sc.nextLine(); // descartar la línea basura completa
            }
        }
    }

    /**
     * Lee un único carácter que pertenezca al conjunto de opciones válidas.
     * No retorna hasta obtener una entrada válida.
     */
    private char readChar(char... validOptions) {
        while (true) {
            String input = sc.nextLine().trim().toLowerCase();
            if (input.length() == 1) {
                char c = input.charAt(0);
                for (char valid : validOptions) {
                    if (c == valid) return c;
                }
            }
            System.out.print("Opción no válida. Introduce una de [w/a/s/d/i/q]: ");
        }
    }
}