package Map;

import Characters.Enemy;
import Characters.Boss;
import Characters.factory.EnemyFactory;
import Inventory.Item;
import Inventory.Potion;
import Inventory.StrengthElixir;
import Inventory.Weapon;
import Inventory.Armor;

import java.util.Random;

import java.io.Serializable;

public class Map implements Serializable {
    // =========================================================
    //  ESTADO DEL MAPA Y POSICIÓN DEL JUGADOR
    // =========================================================

    private final char[][] terrain;
    private final boolean[][] discovered;
    private Enemy[][] enemies;
    private final boolean[][] chests;
    private final int[][] treeRegrowTimers;
    private final int size;
    private int playerX;
    private int playerY;
    private int turnsSinceLastTreeChop;
    private final Random random;

    private static final int FOV_RADIUS = 5;
    private static final int TREE_CHOP_COOLDOWN_TURNS = 5;
    private static final int TREE_REGROW_TURNS = 3;
    private static final int ENEMY_CHASE_RADIUS = 6;
    private static final double ENEMY_SPAWN_CHANCE = 0.05;
    private static final double CHEST_SPAWN_CHANCE = 0.04;

    // Paleta de colores ANSI (Estilo terminal clásica)
    private static final String RESET = "\u001B[0m";
    private static final String GRAY = "\u001B[90m";
    private static final String WHITE = "\u001B[37m";
    private static final String GREEN = "\u001B[32m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BLUE = "\u001B[34m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String DIM = "\u001B[2m";

    public Map(int size, int floorLevel) {
        this.size = size;
        this.terrain = new char[size][size];
        this.discovered = new boolean[size][size];
        this.enemies = new Enemy[size][size];
        this.chests = new boolean[size][size];
        this.treeRegrowTimers = new int[size][size];
        this.random = new Random();
        playerX = size / 2;
        playerY = size / 2;
        turnsSinceLastTreeChop = TREE_CHOP_COOLDOWN_TURNS;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double rand = Math.random();

                // Generación de terreno tipo Dwarf Fortress
                if (rand < 0.10) {
                    terrain[i][j] = '~'; // Agua profunda
                } else if (rand < 0.25) {
                    terrain[i][j] = '^'; // Montaña
                } else if (rand < 0.40) {
                    terrain[i][j] = 'T'; // Árbol robusto
                } else if (rand < 0.50) {
                    terrain[i][j] = 't'; // Árbol pequeño
                } else if (rand < 0.70) {
                    terrain[i][j] = ','; // Hierba alta
                } else {
                    terrain[i][j] = '.'; // Suelo normal
                }

                // Enemigos solo en suelo transitable
                if ((terrain[i][j] == '.' || terrain[i][j] == ',') &&
                        Math.random() < ENEMY_SPAWN_CHANCE &&
                        !(i == playerX && j == playerY)) {
                    enemies[i][j] = EnemyFactory.createRandom(floorLevel);
                } else if ((terrain[i][j] == '.' || terrain[i][j] == ',') &&
                        Math.random() < CHEST_SPAWN_CHANCE &&
                        !(i == playerX && j == playerY)) {
                    chests[i][j] = true;
                }
            }
        }

        // Zona segura para el jugador
        terrain[playerX][playerY] = '.';

        // Generar escalera de salida (>)
        int sx, sy;
        do {
            sx = random.nextInt(size);
            sy = random.nextInt(size);
        } while (terrain[sx][sy] == '~' || terrain[sx][sy] == '^' || (sx == playerX && sy == playerY));
        terrain[sx][sy] = '>';

        if (floorLevel % 5 == 0) {
            enemies[sx][sy] = EnemyFactory.createBoss(floorLevel);
        }

        // Asegura que cualquier enemigo generado tenga un camino hasta el jugador.
        ensurePathsToAllEnemies();
        revealAroundPlayer();
    }

    public void draw() {
        System.out.println("\n" + WHITE + "=== REINO OLVIDADO ===" + RESET);
        String[] legend = buildLegendLines();

        for (int i = 0; i < size; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < size; j++) {
                boolean inFov = isInFov(i, j);
                if (inFov) discovered[i][j] = true;

                if (!discovered[i][j]) {
                    row.append("  ");
                    continue;
                }

                char visible = getVisibleCell(i, j);
                if (inFov) {
                    appendColoredCell(row, visible, false);
                } else {
                    // Memoria del mapa: fuera del FoV se ven casillas ya descubiertas, pero atenuadas.
                    appendColoredCell(row, terrain[i][j], true);
                }
            }
            if (i < legend.length) {
                row.append("   ").append(legend[i]);
            }
            System.out.println(row);
        }
    }

    // =========================================================
    //  LEYENDA DEL MAPA (CHULETA VISUAL)
    // =========================================================

    private String[] buildLegendLines() {
        return new String[] {
                WHITE + "Leyenda:" + RESET,
                YELLOW + "@ " + RESET + "Jugador",
                RED + "g " + RESET + "Enemigo",
                YELLOW + "C " + RESET + "Cofre (botin aleatorio)",
                GRAY + "^ " + RESET + "Montana (bloquea paso)",
                GREEN + "T " + RESET + "Arbol grande (bloquea paso)",
                GREEN + "t " + RESET + "Arbol pequeno (transitable)",
                BLUE + "~ " + RESET + "Agua (bloquea paso)",
                BRIGHT_GREEN + ". " + RESET + "Suelo normal",
                BRIGHT_GREEN + ", " + RESET + "Hierba alta",
                WHITE + "> " + RESET + "Escaleras (Siguiente Piso)",
                WHITE + "FoV: radio " + FOV_RADIUS + " casillas" + RESET,
                WHITE + "Tala: 1 arbol grande cada " + TREE_CHOP_COOLDOWN_TURNS + " turnos" + RESET,
                WHITE + "Regrowth: t -> T cada " + TREE_REGROW_TURNS + " turnos" + RESET,
        };
    }

    public Enemy checkEnemy() {
        return enemies[playerX][playerY];
    }

    public void removeEnemy() {
        enemies[playerX][playerY] = null;
    }

    public void movePlayer(char dir) {
        turnsSinceLastTreeChop++;
        updateTreeRegrowth();

        int newX = playerX;
        int newY = playerY;

        if (dir == 'w' && playerX > 0) newX--;
        if (dir == 's' && playerX < size - 1) newX++;
        if (dir == 'a' && playerY > 0) newY--;
        if (dir == 'd' && playerY < size - 1) newY++;

        // Obstáculos: no puedes pisar agua (~) ni montañas (^).
        // Los árboles grandes (T) se pueden talar cada X turnos.
        char targetTerrain = terrain[newX][newY];
        if (targetTerrain == 'T') {
            if (turnsSinceLastTreeChop >= TREE_CHOP_COOLDOWN_TURNS) {
                markTreeAsChopped(newX, newY);
                turnsSinceLastTreeChop = 0;
                System.out.println(GREEN + "Talas el arbol grande y abres camino." + RESET);
                targetTerrain = 't';
            } else {
                int remaining = TREE_CHOP_COOLDOWN_TURNS - turnsSinceLastTreeChop;
                System.out.println(GRAY + "Arbol demasiado robusto. Podras talar en " + remaining + " turno(s)." + RESET);
                return;
            }
        }

        if (targetTerrain != '~' && targetTerrain != '^') {
            playerX = newX;
            playerY = newY;
            revealAroundPlayer();
        } else {
            System.out.println(GRAY + "El camino está bloqueado por el terreno..." + RESET);
        }
    }

    public Item collectChestLoot() {
        if (!chests[playerX][playerY]) return null;
        chests[playerX][playerY] = false;

        double roll = random.nextDouble();
        if (roll < 0.40) {
            int heal = 25 + random.nextInt(21); // 25-45
            return new Potion("Pocion de vida", heal);
        } else if (roll < 0.70) {
            int atk = 3 + random.nextInt(4); // 3-6
            return new StrengthElixir(atk);
        } else if (roll < 0.85) {
            int wBonus = 5 + random.nextInt(6); // 5-10
            return new Weapon("Espada Olvidada +"+wBonus, "Un arma afilada", wBonus);
        } else {
            int dBonus = 3 + random.nextInt(5); // 3-7
            return new Armor("Cota Oxidada +"+dBonus, "Proteccion adicional", dBonus);
        }
    }

    public boolean isOnStairs() {
        return terrain[playerX][playerY] == '>';
    }

    public void moveEnemies() {
        Enemy[][] nextEnemies = new Enemy[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Enemy enemy = enemies[i][j];
                if (enemy == null) continue;

                int[] nextPos = chooseEnemyStep(i, j);
                int nx = nextPos[0];
                int ny = nextPos[1];

                // Si dos enemigos eligen la misma casilla, el segundo se queda.
                if (nextEnemies[nx][ny] == null) {
                    nextEnemies[nx][ny] = enemy;
                } else if (nextEnemies[i][j] == null) {
                    nextEnemies[i][j] = enemy;
                }
            }
        }
        enemies = nextEnemies;
    }

    // =========================================================
    //  GARANTÍA DE CAMINOS HACIA ENEMIGOS
    // =========================================================

    private void ensurePathsToAllEnemies() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (enemies[i][j] != null && !hasPathTo(i, j)) {
                    carvePathTo(i, j);
                }
            }
        }
    }

    private boolean hasPathTo(int targetX, int targetY) {
        boolean[][] visited = new boolean[size][size];
        int[] queueX = new int[size * size];
        int[] queueY = new int[size * size];
        int head = 0;
        int tail = 0;

        queueX[tail] = playerX;
        queueY[tail] = playerY;
        tail++;
        visited[playerX][playerY] = true;

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (head < tail) {
            int x = queueX[head];
            int y = queueY[head];
            head++;

            if (x == targetX && y == targetY) {
                return true;
            }

            for (int k = 0; k < 4; k++) {
                int nx = x + dx[k];
                int ny = y + dy[k];
                if (isInside(nx, ny) && !visited[nx][ny] && isWalkableForPath(nx, ny, targetX, targetY)) {
                    visited[nx][ny] = true;
                    queueX[tail] = nx;
                    queueY[tail] = ny;
                    tail++;
                }
            }
        }

        return false;
    }

    private boolean isWalkableForPath(int x, int y, int targetX, int targetY) {
        if (x == targetX && y == targetY) return true;
        char t = terrain[x][y];
        return t == '.' || t == ',' || t == 't';
    }

    private void carvePathTo(int targetX, int targetY) {
        int x = playerX;
        int y = playerY;

        while (x != targetX) {
            x += Integer.compare(targetX, x);
            clearCellForPath(x, y, targetX, targetY);
        }
        while (y != targetY) {
            y += Integer.compare(targetY, y);
            clearCellForPath(x, y, targetX, targetY);
        }
    }

    private void clearCellForPath(int x, int y, int targetX, int targetY) {
        if (x == targetX && y == targetY) return;
        terrain[x][y] = '.';
        treeRegrowTimers[x][y] = 0;
    }

    // =========================================================
    //  DINÁMICA DE CRECIMIENTO DE ÁRBOLES
    // =========================================================

    private void markTreeAsChopped(int x, int y) {
        terrain[x][y] = 't';
        treeRegrowTimers[x][y] = TREE_REGROW_TURNS;
    }

    private void updateTreeRegrowth() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (treeRegrowTimers[i][j] <= 0) continue;

                treeRegrowTimers[i][j]--;
                if (treeRegrowTimers[i][j] > 0) continue;

                // Si el jugador está encima, esperamos 1 turno más para no encerrarlo.
                if (i == playerX && j == playerY) {
                    treeRegrowTimers[i][j] = 1;
                    continue;
                }

                terrain[i][j] = 'T';
            }
        }
    }

    private int[] chooseEnemyStep(int x, int y) {
        int currentDist = Math.abs(playerX - x) + Math.abs(playerY - y);
        boolean chase = currentDist <= ENEMY_CHASE_RADIUS;

        int[][] dirs = { {-1,0}, {1,0}, {0,-1}, {0,1} };

        if (chase) {
            int bestDist = currentDist;
            int bestX = x;
            int bestY = y;

            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (!isInside(nx, ny) || !isWalkableForEnemy(nx, ny)) continue;
                int dist = Math.abs(playerX - nx) + Math.abs(playerY - ny);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = nx;
                    bestY = ny;
                }
            }
            return new int[] {bestX, bestY};
        }

        // Movimiento aleatorio cuando no detecta al jugador.
        for (int tries = 0; tries < 5; tries++) {
            int pick = random.nextInt(5);
            if (pick == 4) return new int[] {x, y}; // quedarse quieto
            int nx = x + dirs[pick][0];
            int ny = y + dirs[pick][1];
            if (isInside(nx, ny) && isWalkableForEnemy(nx, ny)) {
                return new int[] {nx, ny};
            }
        }
        return new int[] {x, y};
    }

    private boolean isWalkableForEnemy(int x, int y) {
        char t = terrain[x][y];
        return t != '~' && t != '^' && t != 'T';
    }

    private void revealAroundPlayer() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (isInFov(i, j)) {
                    discovered[i][j] = true;
                }
            }
        }
    }

    private boolean isInFov(int x, int y) {
        int dx = x - playerX;
        int dy = y - playerY;
        return (dx * dx + dy * dy) <= (FOV_RADIUS * FOV_RADIUS);
    }

    private char getVisibleCell(int x, int y) {
        if (x == playerX && y == playerY) return '@';
        if (enemies[x][y] != null) return 'g';
        if (chests[x][y]) return 'C';
        return terrain[x][y];
    }

    private void appendColoredCell(StringBuilder row, char cell, boolean dimmed) {
        String prefix = dimmed ? DIM : "";
        switch (cell) {
            case '@' -> row.append(prefix).append(YELLOW).append(cell).append(" ").append(RESET);
            case 'g' -> row.append(prefix).append(RED).append(cell).append(" ").append(RESET);
            case 'C' -> row.append(prefix).append(YELLOW).append(cell).append(" ").append(RESET);
            case '>' -> row.append(prefix).append(WHITE).append(cell).append(" ").append(RESET);
            case '^' -> row.append(prefix).append(GRAY).append(cell).append(" ").append(RESET);
            case 'T', 't' -> row.append(prefix).append(GREEN).append(cell).append(" ").append(RESET);
            case '~' -> row.append(prefix).append(BLUE).append(cell).append(" ").append(RESET);
            case '.', ',' -> row.append(prefix).append(BRIGHT_GREEN).append(cell).append(" ").append(RESET);
            default -> row.append(prefix).append(WHITE).append(cell).append(" ").append(RESET);
        }
    }

    private boolean isInside(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    // =========================================================
    //  SERIALIZACIÓN PARA WEB (sin ANSI)
    // =========================================================

    /**
     * Returns the map grid ready for JSON serialization.
     * Each cell format: "TYPE:VISIBILITY"
     * Visibility: VISIBLE | DIM | HIDDEN
     * Types: PLAYER, ENEMY, BOSS, CHEST, STAIRS, FLOOR, GRASS,
     *        TREE_BIG, TREE_SMALL, MOUNTAIN, WATER
     */
    public String[][] getGridForWeb() {
        String[][] grid = new String[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                String visibility;
                if (!discovered[i][j]) {
                    grid[i][j] = "HIDDEN:HIDDEN";
                    continue;
                }
                if (isInFov(i, j)) {
                    visibility = "VISIBLE";
                } else {
                    visibility = "DIM";
                }

                String cellType;
                if (i == playerX && j == playerY) {
                    cellType = "PLAYER";
                } else if (isInFov(i, j) && enemies[i][j] != null) {
                    // Distinguish boss from regular enemy by class name
                    cellType = enemies[i][j].getClass().getSimpleName().equals("Boss") ? "BOSS" : "ENEMY";
                } else if (isInFov(i, j) && chests[i][j]) {
                    cellType = "CHEST";
                } else {
                    cellType = switch (terrain[i][j]) {
                        case '.' -> "FLOOR";
                        case ',' -> "GRASS";
                        case 'T' -> "TREE_BIG";
                        case 't' -> "TREE_SMALL";
                        case '^' -> "MOUNTAIN";
                        case '~' -> "WATER";
                        case '>' -> "STAIRS";
                        default  -> "FLOOR";
                    };
                }
                grid[i][j] = cellType + ":" + visibility;
            }
        }
        return grid;
    }
}