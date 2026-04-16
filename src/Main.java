import java.util.Scanner;
import Management.FileManager;

public class Main {
    // =========================================================
    //  PUNTO DE ENTRADA DE LA APLICACIÓN
    // =========================================================

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int option = 0;

        while (option != 3) {
            System.out.println("\n==================================");
            System.out.println("        PROYECTO IA RPG           ");
            System.out.println("==================================");
            System.out.println("1. Nueva Partida");
            System.out.println("2. Cargar Partida");
            System.out.println("3. Salir");
            System.out.print("Elige una opción: ");

            if (scanner.hasNextInt()) {
                option = scanner.nextInt();
                scanner.nextLine();

                if (option == 1) {
                    Game game = new Game();
                    game.start();
                } else if (option == 2) {
                    Game game = (Game) FileManager.loadGame();
                    if (game != null) {
                        game.restoreTransientState();
                        game.start();
                    } else {
                        System.out.println("No se pudo cargar la partida o no existe.");
                    }
                } else if (option == 3) {
                    System.out.println("Saliendo...");
                } else {
                    System.out.println("Opción inválida.");
                }
            } else {
                System.out.println("Entrada inválida.");
                scanner.nextLine();
            }
        }
    }
}