package Combat;

import Characters.Player;
import Characters.Enemy;
import Characters.Character;

import java.util.Random;
import java.util.Scanner;

public class Combat {
    // =========================================================
    //  CONFIGURACIÓN VISUAL Y FLUJO DE COMBATE
    // =========================================================

    // Códigos ANSI para el texto
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String PURPLE = "\u001B[35m";
    private static final Random RNG = new Random();
    private static final int POISON_TURNS = 3;
    private static final int POISON_DAMAGE = 6;
    private static final double POISON_CHANCE_PLAYER = 0.20;
    private static final double POISON_CHANCE_ENEMY = 0.12;

    public static void fight(Player player, Enemy enemy, Scanner sc) {
        System.out.println("\n" + RED + "⚔️ ¡COMBATE INICIADO! ⚔️" + RESET);
        System.out.println("Te enfrentas a un salvaje " + RED + enemy.getName() + RESET + "!");
        delay(1000); // Pausa de 1 segundo para crear tensión
        int enemyPoisonTurns = 0;
        int playerPoisonTurns = 0;

        while (player.isAlive() && enemy.isAlive()) {
            if (enemyPoisonTurns > 0) {
                enemyPoisonTurns = applyPoisonTick(enemy, enemyPoisonTurns);
            }
            if (!enemy.isAlive()) {
                System.out.println(GREEN + "💀 ¡" + enemy.getName() + " cae por veneno!" + RESET);
                break;
            }

            if (playerPoisonTurns > 0) {
                playerPoisonTurns = applyPoisonTick(player, playerPoisonTurns);
            }
            if (!player.isAlive()) {
                break;
            }

            drawCombatHUD(player, enemy);

            System.out.print(YELLOW + "\nElige acción (1-Atacar | 2-Especial): " + RESET);
            int choice = readIntSafe(sc);
            System.out.println("----------------------------------------");

            // Turno del Jugador
            if (choice == 1) {
                System.out.println("🗡️ ¡Atacas a " + enemy.getName() + "!");
                enemy.takeDamage(player.attack());
                if (enemy.isAlive() && RNG.nextDouble() < POISON_CHANCE_PLAYER) {
                    enemyPoisonTurns = Math.max(enemyPoisonTurns, POISON_TURNS);
                    System.out.println(PURPLE + enemy.getName() + " queda envenenado." + RESET);
                }
            } else if (choice == 2) {
                try {
                    player.special(enemy);
                    if (enemy.isAlive() && RNG.nextDouble() < POISON_CHANCE_PLAYER) {
                        enemyPoisonTurns = Math.max(enemyPoisonTurns, POISON_TURNS);
                        System.out.println(PURPLE + enemy.getName() + " queda envenenado." + RESET);
                    }
                } catch (IllegalStateException e) {
                    System.out.println(RED + "Ataque fallido por falta de maná. Pierdes el turno." + RESET);
                }
            } else {
                System.out.println("Opción inválida. Tropiezas y pierdes el turno.");
            }

            delay(800); // Pausa antes de que el enemigo responda

            // Si el enemigo muere por tu ataque, cortamos el bucle
            if (!enemy.isAlive()) {
                System.out.println(GREEN + "💀 ¡" + enemy.getName() + " ha sido derrotado!" + RESET);
                break;
            }

            // Turno del Enemigo
            System.out.println("\n🛡️ ¡" + enemy.getName() + " contraataca!");
            delay(500);
            player.takeDamage(enemy.attack());
            if (player.isAlive() && RNG.nextDouble() < POISON_CHANCE_ENEMY) {
                playerPoisonTurns = Math.max(playerPoisonTurns, POISON_TURNS);
                System.out.println(PURPLE + "Has sido envenenado." + RESET);
            }
            delay(800);
        }

        // Fin del combate
        if (player.isAlive()) {
            System.out.println(GREEN + "\n✨ ¡VICTORIA! ✨" + RESET);
            player.gainExp(50);
        } else {
            System.out.println(RED + "\n☠️ Has sido derrotado..." + RESET);
        }
        delay(1500);
    }

    // --- Métodos de apoyo visual ---

    private static void drawCombatHUD(Player p, Enemy e) {
        System.out.println("\n========================================");
        // Barra del enemigo (Roja)
        System.out.printf("%-10s %s %d/%d%n",
                e.getName(), getBar(e.getHp(), e.getMaxHp(), RED), e.getHp(), e.getMaxHp());

        System.out.println("                 VS");

        // Barra del jugador (Verde) y su Maná (Azul)
        System.out.printf("%-10s %s %d/%d%n",
                p.getName(), getBar(p.getHp(), p.getMaxHp(), GREEN), p.getHp(), p.getMaxHp());
        System.out.printf("%-10s %s %d/%d%n",
                "Maná", getBar(p.getMana(), p.getMaxMana(), BLUE), p.getMana(), p.getMaxMana());
        System.out.println("========================================");
    }

    private static String getBar(int current, int max, String color) {
        if (max <= 0) return "[          ]";
        int fill = (int) Math.round(((double) current / max) * 10);
        fill = Math.max(0, Math.min(10, fill)); // Asegurar que esté entre 0 y 10
        int empty = 10 - fill;

        return color + "[" + "█".repeat(fill) + "░".repeat(empty) + "]" + RESET;
    }

    private static int applyPoisonTick(Character target, int turnsLeft) {
        System.out.println(PURPLE + target.getName() + " sufre " + POISON_DAMAGE + " de veneno." + RESET);
        target.takeDamage(POISON_DAMAGE);
        return turnsLeft - 1;
    }

    private static int readIntSafe(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.print(RED + "Entrada invalida. Escribe 1 o 2: " + RESET);
            sc.nextLine();
        }
        int value = sc.nextInt();
        sc.nextLine();
        return value;
    }

    private static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}